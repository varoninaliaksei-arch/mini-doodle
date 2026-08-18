package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.MeetingStatus;
import com.minidoodle.domain.scheduling.Participant;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateBookingUseCaseTest {

    private final InMemoryTimeSlotRepository timeSlotRepository = new InMemoryTimeSlotRepository();
    private final InMemoryMeetingRepository meetingRepository = new InMemoryMeetingRepository();
    private final InMemoryOutboxEventRepository outboxEventRepository = new InMemoryOutboxEventRepository();
    private final CreateBookingUseCase useCase = new CreateBookingUseCase(timeSlotRepository, meetingRepository,
            outboxEventRepository, new ObjectMapper());

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));
    private final MeetingDetails details = new MeetingDetails("Sync", "weekly sync", UUID.randomUUID());
    private final List<Participant> participants = List.of(new Participant("a@example.com", "Alice", null));

    @Test
    void booksTheSlotSchedulesTheMeetingAndWritesTheOutboxEvent() {
        TimeSlot slot = TimeSlot.free(UUID.randomUUID(), UUID.randomUUID(), INTERVAL);
        timeSlotRepository.seed(slot);

        Meeting meeting = useCase.execute(slot.id(), details, participants, "key-1");

        assertEquals(MeetingStatus.SCHEDULED, meeting.status());
        assertEquals(SlotStatus.BOOKED, timeSlotRepository.findById(slot.id()).orElseThrow().status());
        assertEquals(1, outboxEventRepository.saved.size());
        assertEquals("MeetingBooked", outboxEventRepository.saved.get(0).type());
        assertEquals(meeting.id(), outboxEventRepository.saved.get(0).aggregateId());
    }

    @Test
    void returnsExistingMeetingWhenIdempotencyKeyAlreadyUsedWithoutTouchingTheSlot() {
        TimeSlot slot = TimeSlot.free(UUID.randomUUID(), UUID.randomUUID(), INTERVAL);
        timeSlotRepository.seed(slot);
        Meeting existing = Meeting.schedule(slot.id(), details, participants, "key-1");
        existing.pullEvents();
        meetingRepository.seed(existing);

        Meeting result = useCase.execute(slot.id(), details, participants, "key-1");

        assertSame(existing, result);
        assertEquals(SlotStatus.FREE, timeSlotRepository.findById(slot.id()).orElseThrow().status());
        assertTrue(outboxEventRepository.saved.isEmpty());
    }

    @Test
    void slotNotFoundThrows() {
        assertThrows(SlotNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), details, participants, "key-1"));
    }

    @Test
    void bookingAnAlreadyBookedSlotPropagatesIllegalStateException() {
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), UUID.randomUUID(), INTERVAL, SlotStatus.BOOKED, 0L);
        timeSlotRepository.seed(slot);

        assertThrows(IllegalStateException.class,
                () -> useCase.execute(slot.id(), details, participants, "key-1"));
    }
}
