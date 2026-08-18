package com.minidoodle.infrastructure.scheduling.web;

import java.time.Duration;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateSlotsBulkRequest(
        Instant startsAt,
        Instant endsAt,
        @Schema(description = "Slots are generated back-to-back from startsAt; a trailing remainder shorter "
                + "than one slotDuration is discarded, never rounded up. Capped at 500 slots per call — "
                + "larger requests are rejected outright (422), not truncated.")
        Duration slotDuration) {
}
