package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

/** organizerId is not a field here — it comes from X-User-Id, per TECH-6. */
public record CreateBookingRequest(UUID slotId, String title, String description,
        List<ParticipantRequest> participants) {
}
