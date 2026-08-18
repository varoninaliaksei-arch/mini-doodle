package com.minidoodle.application.scheduling.exception;

import java.time.Duration;

/**
 * Free/busy and slot-listing queries cap their window (~90 days) to
 * protect the read path. Maps to {@code 400} at the REST layer — the
 * request is rejected outright, never silently truncated.
 */
public class WindowTooLargeException extends RuntimeException {

    public WindowTooLargeException(Duration requestedWindow, Duration maxWindow) {
        super("Requested window of %s exceeds the max of %s".formatted(requestedWindow, maxWindow));
    }
}
