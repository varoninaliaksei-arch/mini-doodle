package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /meetings/{id}/cancel: meeting.cancel() then slot.release() — DOM-2's
 * only legal caller of {@link TimeSlot#release()}.
 */
public class CancelMeetingUseCase {

    private final MeetingRepository meetingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventFactory outboxEventFactory;

    public CancelMeetingUseCase(MeetingRepository meetingRepository, TimeSlotRepository timeSlotRepository,
            OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.meetingRepository = meetingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.outboxEventFactory = new OutboxEventFactory(objectMapper);
    }

    @Transactional
    public Meeting execute(UUID meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        meeting.cancel();

        TimeSlot slot = timeSlotRepository.findById(meeting.slotId())
                .orElseThrow(() -> new SlotNotFoundException(meeting.slotId()));
        slot.release();

        Meeting savedMeeting = meetingRepository.save(meeting);
        timeSlotRepository.save(slot);

        meeting.pullEvents().forEach(event -> outboxEventRepository.save(outboxEventFactory.from(event)));

        return savedMeeting;
    }
}
