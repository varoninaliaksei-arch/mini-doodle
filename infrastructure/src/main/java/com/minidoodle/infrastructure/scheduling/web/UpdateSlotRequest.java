package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/** PATCH /slots/{id}: both fields optional, {@code null} means "unchanged". */
public record UpdateSlotRequest(
        @Schema(description = "New start of the half-open interval; omit/null to keep the current value.")
        Instant startsAt,
        @Schema(description = "New end of the half-open interval; omit/null to keep the current value.")
        Instant endsAt,
        @Schema(description = "The slot's version as last read by the caller; a mismatch means it changed "
                + "concurrently and the request is rejected with 409.")
        long version) {
}
