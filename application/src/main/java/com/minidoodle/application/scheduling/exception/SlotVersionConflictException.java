package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/**
 * Optimistic-lock conflict on a slot's {@code version} — distinct from
 * {@link SlotBookedException} so the REST layer can tell "someone else
 * changed this concurrently" apart from "this slot is booked".
 */
public class SlotVersionConflictException extends RuntimeException {

    public SlotVersionConflictException(UUID slotId, long expectedVersion, long actualVersion) {
        super("Slot %s has version %d but caller expected %d".formatted(slotId, actualVersion, expectedVersion));
    }

    public SlotVersionConflictException(UUID slotId, Throwable cause) {
        super("Slot %s was modified concurrently".formatted(slotId), cause);
    }
}
