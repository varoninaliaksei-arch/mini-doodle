package com.minidoodle.application.scheduling;

import java.time.Duration;

import com.minidoodle.application.scheduling.exception.WindowTooLargeException;
import com.minidoodle.domain.scheduling.TimeInterval;

/** TECH-7: shared ~90-day window cap for GET /slots and GET /availability. */
final class WindowPolicy {

    static final Duration MAX_WINDOW = Duration.ofDays(90);

    private WindowPolicy() {
    }

    static void enforce(TimeInterval window) {
        Duration requested = Duration.between(window.start(), window.end());
        if (requested.compareTo(MAX_WINDOW) > 0) {
            throw new WindowTooLargeException(requested, MAX_WINDOW);
        }
    }
}
