package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/**
 * A BLOCKED slot isn't bookable, but the reason is distinct from
 * {@link SlotConflictException}: no one else booked it — the owner
 * manually blocked it. Maps to {@code 409} at the REST layer with a
 * message naming the actual cause instead of the generic conflict one.
 */
public class SlotBlockedException extends RuntimeException {

    public SlotBlockedException(UUID slotId) {
        super("Slot %s is blocked by its owner".formatted(slotId));
    }
}
