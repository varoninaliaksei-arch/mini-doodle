package com.minidoodle.domain.scheduling;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minidoodle.domain.scheduling.events.MeetingBooked;
import com.minidoodle.domain.scheduling.events.MeetingCancelled;
import com.minidoodle.domain.scheduling.events.MeetingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeetingTest {

    private final UUID slotId = UUID.randomUUID();
    private final MeetingDetails details = new MeetingDetails("Sync", "weekly sync", UUID.randomUUID());
    private final List<Participant> participants = List.of(new Participant("a@example.com", "Alice", null));

    @Test
    void scheduleReturnsScheduledMeetingAndRecordsMeetingBooked() {
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");

        assertEquals(MeetingStatus.SCHEDULED, meeting.status());
        assertEquals(slotId, meeting.slotId());

        List<MeetingEvent> events = meeting.pullEvents();
        assertEquals(1, events.size());
        MeetingBooked booked = assertInstanceOf(MeetingBooked.class, events.get(0));
        assertEquals(meeting.id(), booked.meetingId());
        assertEquals(slotId, booked.slotId());
    }

    @Test
    void cancelTransitionsScheduledToCancelledAndRecordsMeetingCancelled() {
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");
        meeting.pullEvents();

        meeting.cancel();

        assertEquals(MeetingStatus.CANCELLED, meeting.status());

        List<MeetingEvent> events = meeting.pullEvents();
        assertEquals(1, events.size());
        MeetingCancelled cancelled = assertInstanceOf(MeetingCancelled.class, events.get(0));
        assertEquals(meeting.id(), cancelled.meetingId());
        assertEquals(slotId, cancelled.slotId());
    }

    @Test
    void cancelOfAlreadyCancelledMeetingThrows() {
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");
        meeting.cancel();

        assertThrows(IllegalStateException.class, meeting::cancel);
    }

    @Test
    void pullEventsDrainsTheEventList() {
        Meeting meeting = Meeting.schedule(slotId, details, participants, "key-1");

        assertTrue(meeting.pullEvents().size() == 1);
        assertTrue(meeting.pullEvents().isEmpty());
    }
}
