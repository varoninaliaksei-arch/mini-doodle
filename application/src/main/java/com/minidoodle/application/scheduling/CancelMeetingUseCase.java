package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingStatus;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /meetings/{id}/cancel: meeting.cancel() then slot.release() — the
 * only legal caller of {@link TimeSlot#release()}. Owner-only, checked
 * against the Meeting's own organizerId (the caller who created the
 * booking), not the slot's calendarId — cancelling is an action on the
 * Meeting aggregate, so it checks that aggregate's own owner reference,
 * the same way slot mutations check TimeSlot.calendarId(). See README
 * "Assumptions & trade-offs" for the uniform ownership-check policy this
 * is part of.
 *
 * <p>"At most one active meeting per slot" (what TimeSlot.book()'s
 * FREE-only precondition is meant to guarantee) has no database backstop
 * the way slot overlap does — {@code meetings.slot_id} has no uniqueness
 * constraint, since a slot legitimately accumulates multiple historical
 * (cancelled) meetings over its lifetime. Before releasing the slot, this
 * checks that no *other* {@code SCHEDULED} meeting exists for it, refusing
 * to cancel rather than silently freeing a slot another active meeting
 * still depends on.
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
    public Meeting execute(UUID meetingId, UUID callerId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        if (!meeting.details().organizerId().equals(callerId)) {
            throw new NotOwnerException("meeting", meetingId);
        }

        boolean anotherActiveMeetingOnSameSlot = meetingRepository.findBySlotId(meeting.slotId()).stream()
                .anyMatch(other -> !other.id().equals(meeting.id()) && other.status() == MeetingStatus.SCHEDULED);
        if (anotherActiveMeetingOnSameSlot) {
            throw new IllegalStateException(
                    "Slot %s has more than one active meeting; refusing to cancel %s"
                            .formatted(meeting.slotId(), meetingId));
        }

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
