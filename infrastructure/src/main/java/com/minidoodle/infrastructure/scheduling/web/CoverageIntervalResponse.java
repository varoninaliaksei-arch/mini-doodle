package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

import com.minidoodle.domain.scheduling.Coverage;

public record CoverageIntervalResponse(
        @Schema(description = "Inclusive start of the half-open interval [start, end).") Instant start,
        @Schema(description = "Exclusive end of the half-open interval [start, end).") Instant end,
        @Schema(description = "FREE/BUSY are derived from the owner's slots; UNAVAILABLE means no slot is "
                + "defined for that stretch at all. The source of BUSY (a block vs. a booking) is never "
                + "exposed — this is a privacy-reduced projection.")
        Coverage coverage) {
}
