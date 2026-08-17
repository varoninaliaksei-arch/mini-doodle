package com.minidoodle.domain.scheduling.events;

import java.util.UUID;

public record MeetingBooked(UUID meetingId, UUID slotId) implements MeetingEvent {
}
