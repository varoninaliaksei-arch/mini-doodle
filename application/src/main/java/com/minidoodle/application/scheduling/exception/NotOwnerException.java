package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/**
 * Caller's {@code X-User-Id} does not match the resource's recorded owner
 * ({@code TimeSlot.calendarId()} for slots, {@code Meeting}'s organizerId
 * for meetings). Maps to {@code 403} at the REST layer.
 */
public class NotOwnerException extends RuntimeException {

    public NotOwnerException(String resourceType, UUID resourceId) {
        super("Caller does not own %s %s".formatted(resourceType, resourceId));
    }
}
