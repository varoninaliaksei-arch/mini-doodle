package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/** Maps to {@code 404} at the REST layer per 02-ARCHITECTURE.md §4. */
public class MeetingNotFoundException extends RuntimeException {

    public MeetingNotFoundException(UUID meetingId) {
        super("Meeting %s not found".formatted(meetingId));
    }
}
