package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/** Maps to {@code 404} at the REST layer. */
public class SlotNotFoundException extends RuntimeException {

    public SlotNotFoundException(UUID slotId) {
        super("Slot %s not found".formatted(slotId));
    }
}
