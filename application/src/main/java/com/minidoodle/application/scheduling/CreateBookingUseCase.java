package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.Participant;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /bookings orchestration: idempotency-key short-circuit,
 * slot.book() + Meeting.schedule() as one transaction, outbox write.
 * The outbox publisher itself is a separate, later task.
 */
public class CreateBookingUseCase {

    private final TimeSlotRepository timeSlotRepository;
    private final MeetingRepository meetingRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    public CreateBookingUseCase(TimeSlotRepository timeSlotRepository, MeetingRepository meetingRepository,
            OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.timeSlotRepository = timeSlotRepository;
        this.meetingRepository = meetingRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventFactory = new OutboxEventFactory(objectMapper);
    }

    @Transactional
    public Meeting execute(UUID slotId, MeetingDetails details, List<Participant> participants,
            String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<Meeting> existing = meetingRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        TimeSlot slot = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));

        try {
            slot.book();
        } catch (IllegalStateException e) {
            // Same condition as the race below (someone already has this
            // slot), just caught earlier via the loaded status rather than
            // at save() via the exclusion constraint/optimistic lock. Both
            // must surface as the same exception so callers/metrics don't
            // have to distinguish a cause that isn't theirs to care about.
            throw new SlotConflictException(slotId, e);
        }
        Meeting meeting = Meeting.schedule(slotId, details, participants, idempotencyKey);

        try {
            timeSlotRepository.save(slot);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Lost the race for this slot. Once JPA raises this, the current
            // transaction is rollback-only regardless of whether this catch
            // block handles it — there's no reading the winner's row from
            // inside a poisoned transaction. A clean 409 is the standard
            // contract here (cf. Stripe's Idempotency-Key semantics): the
            // caller retries with the same key and gets the winner's
            // meeting back via the short-circuit above, in a fresh
            // transaction. Same exception type as the exclusion-constraint
            // conflict — both mean "this slot is already spoken for", just
            // caught at different layers, so clients see the same
            // /problems/slot-conflict either way.
            throw new SlotConflictException(slotId, e);
        }

        Meeting saved = meetingRepository.save(meeting);

        meeting.pullEvents().forEach(event -> outboxEventRepository.save(outboxEventFactory.from(event)));

        return saved;
    }
}
