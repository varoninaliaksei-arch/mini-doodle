package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

import com.minidoodle.domain.scheduling.TimeInterval;

/**
 * TECH-1: the exclusion constraint rejected an overlapping slot. Maps to
 * {@code 409} at the REST layer.
 */
public class SlotConflictException extends RuntimeException {

    public SlotConflictException(UUID calendarId, TimeInterval interval, Throwable cause) {
        super("Slot %s overlaps an existing slot in calendar %s".formatted(interval, calendarId), cause);
    }
}
