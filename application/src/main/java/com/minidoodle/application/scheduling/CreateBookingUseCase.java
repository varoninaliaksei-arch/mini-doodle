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
 * POST /bookings orchestration: idempotency-key short-circuit (SCOPE-1),
 * slot.book() + Meeting.schedule() as one transaction, outbox write
 * (INFRA-3). The outbox publisher itself is a separate, later task.
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

        slot.book();
        Meeting meeting = Meeting.schedule(slotId, details, participants, idempotencyKey);

        try {
            timeSlotRepository.save(slot);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Lost the race for this slot. Once JPA raises this, the
            // current transaction is marked rollback-only regardless of
            // whether this catch block handles it - there is no reading
            // the winner's row from inside a poisoned transaction. A clean
            // 409 is the correct, standard contract here (matching how
            // Idempotency-Key races are documented elsewhere, e.g.
            // Stripe's API): the caller retries with the same key and gets
            // the winner's meeting back through the ordinary short-circuit
            // above, in a fresh transaction. Same exception type as the
            // exclusion-constraint conflict (TECH-1) - both are "this slot
            // is already spoken for", just caught at different layers -
            // so clients get the same /problems/slot-conflict either way.
            throw new SlotConflictException(slotId, e);
        }

        Meeting saved = meetingRepository.save(meeting);

        meeting.pullEvents().forEach(event -> outboxEventRepository.save(outboxEventFactory.from(event)));

        return saved;
    }
}
