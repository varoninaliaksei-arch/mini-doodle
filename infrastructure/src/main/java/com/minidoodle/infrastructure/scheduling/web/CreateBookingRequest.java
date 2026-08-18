package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * organizerId is deliberately absent — it comes from the X-User-Id header,
 * not the body (write on your own behalf only). Likewise the
 * idempotency key travels as the optional Idempotency-Key header, not here.
 */
@Schema(description = "organizerId comes from the X-User-Id header, not this body. An optional "
        + "Idempotency-Key header controls retry-safety.")
public record CreateBookingRequest(UUID slotId, String title, String description,
        List<ParticipantRequest> participants) {
}
