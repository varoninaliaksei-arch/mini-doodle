package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;

import com.minidoodle.domain.scheduling.Coverage;

public record CoverageIntervalResponse(Instant start, Instant end, Coverage coverage) {
}
