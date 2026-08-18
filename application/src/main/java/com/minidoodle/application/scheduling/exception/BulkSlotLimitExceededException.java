package com.minidoodle.application.scheduling.exception;

/**
 * Bulk slot creation is capped at 500 slots per call. Maps to
 * {@code 422} at the REST layer — the request is rejected outright, never
 * silently truncated.
 */
public class BulkSlotLimitExceededException extends RuntimeException {

    public BulkSlotLimitExceededException(long requestedSlotCount, int maxSlotCount) {
        super("Requested %d slots exceeds the max of %d per call".formatted(requestedSlotCount, maxSlotCount));
    }
}
