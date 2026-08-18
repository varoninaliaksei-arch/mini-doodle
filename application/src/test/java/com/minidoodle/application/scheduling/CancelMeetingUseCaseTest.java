package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.MeetingStatus;
import com.minidoodle.domain.scheduling.Participant;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancelMeetingUseCaseTest {

    private final InMemoryMeetingRepository meetingRepository = new InMemoryMeetingRepository();
    private final InMemoryTimeSlotRepository timeSlotRepository = new InMemoryTimeSlotRepository();
    private final InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
    private final CancelMeetingUseCase useCase = new CancelMeetingUseCase(meetingRepository, timeSlotRepository,
            outboxEventRepository, new ObjectMapper());

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));
    private final UUID organizerId = UUID.randomUUID();
    private final MeetingDetails details = new MeetingDetails("Sync", "weekly sync", organizerId);
    private final List<Participant> participants = List.of(new Participant("a@example.com", "Alice", null));

    @Test
    void cancelsTheMeetingReleasesTheSlotAndWritesTheOutboxEvent() {
        UUID slotId = UUID.randomUUID();
        TimeSlot bookedSlot = new TimeSlot(slotId, UUID.randomUUID(), INTERVAL, SlotStatus.BOOKED, 0L);
        timeSlotRepository.seed(bookedSlot);
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");
        meeting.pullEvents();
        meetingRepository.seed(meeting);

        Meeting cancelled = useCase.execute(meeting.id(), organizerId);

        assertEquals(MeetingStatus.CANCELLED, cancelled.status());
        assertEquals(SlotStatus.FREE, timeSlotRepository.findById(slotId).orElseThrow().status());
        assertEquals(1, outboxEventRepository.saved.size());
        assertEquals("MeetingCancelled", outboxEventRepository.saved.get(0).type());
        assertEquals(meeting.id(), outboxEventRepository.saved.get(0).aggregateId());
    }

    @Test
    void meetingNotFoundThrows() {
        assertThrows(MeetingNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), organizerId));
    }

    @Test
    void missingSlotThrows() {
        Meeting meeting = Meeting.schedule(UUID.randomUUID(), details, participants, "key-1");
        meeting.pullEvents();
        meetingRepository.seed(meeting);

        assertThrows(SlotNotFoundException.class, () -> useCase.execute(meeting.id(), organizerId));
    }

    @Test
    void rejectsCallerWhoIsNotTheOrganizer() {
        UUID slotId = UUID.randomUUID();
        TimeSlot bookedSlot = new TimeSlot(slotId, UUID.randomUUID(), INTERVAL, SlotStatus.BOOKED, 0L);
        timeSlotRepository.seed(bookedSlot);
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");
        meeting.pullEvents();
        meetingRepository.seed(meeting);

        assertThrows(NotOwnerException.class, () -> useCase.execute(meeting.id(), UUID.randomUUID()));
        assertEquals(MeetingStatus.SCHEDULED, meetingRepository.findById(meeting.id()).orElseThrow().status());
        assertEquals(SlotStatus.BOOKED, timeSlotRepository.findById(slotId).orElseThrow().status());
    }
}
