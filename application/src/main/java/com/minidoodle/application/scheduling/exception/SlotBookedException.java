package com.minidoodle.application.scheduling.exception;

import java.util.UUID;

/**
 * A booked slot cannot be deleted or modified directly. Maps to
 * {@code 409} at the REST layer with "cancel the meeting first".
 */
public class SlotBookedException extends RuntimeException {

    public SlotBookedException(UUID slotId) {
        super("Slot %s is booked; cancel the meeting first".formatted(slotId));
    }
}
