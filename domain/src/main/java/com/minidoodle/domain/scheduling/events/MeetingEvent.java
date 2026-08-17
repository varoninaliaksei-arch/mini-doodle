package com.minidoodle.domain.scheduling.events;

import java.util.UUID;

public sealed interface MeetingEvent permits MeetingBooked, MeetingCancelled {

    UUID meetingId();

    UUID slotId();
}
