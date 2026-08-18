package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

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

        timeSlotRepository.save(slot);
        Meeting saved = meetingRepository.save(meeting);

        meeting.pullEvents().forEach(event -> outboxEventRepository.save(outboxEventFactory.from(event)));

        return saved;
    }
}
