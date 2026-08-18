package com.minidoodle.domain.scheduling;

import java.time.Instant;

/**
 * Half-open interval [start, end) in UTC.
 */
public record TimeInterval(Instant start, Instant end) {

    public TimeInterval {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start and end must not be null");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start must be before end");
        }
    }

    public boolean overlaps(TimeInterval other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }
}
