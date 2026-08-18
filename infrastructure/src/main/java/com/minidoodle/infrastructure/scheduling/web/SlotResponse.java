package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.minidoodle.domain.scheduling.SlotStatus;

public record SlotResponse(
        UUID id,
        String ownerId,
        @Schema(description = "Inclusive start of the half-open interval [startsAt, endsAt).") Instant startsAt,
        @Schema(description = "Exclusive end of the half-open interval [startsAt, endsAt).") Instant endsAt,
        SlotStatus status,
        @Schema(description = "Optimistic-lock version; echo it back in PATCH/DELETE requests.") long version) {
}
