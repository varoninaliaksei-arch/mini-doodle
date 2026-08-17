package com.minidoodle.domain.scheduling;

import java.util.Objects;

public record CoverageInterval(TimeInterval interval, Coverage coverage) {

    public CoverageInterval {
        Objects.requireNonNull(interval, "interval must not be null");
        Objects.requireNonNull(coverage, "coverage must not be null");
    }
}
