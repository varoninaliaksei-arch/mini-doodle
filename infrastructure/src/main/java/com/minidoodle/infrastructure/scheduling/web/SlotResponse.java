package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.util.UUID;

import com.minidoodle.domain.scheduling.SlotStatus;

public record SlotResponse(UUID id, String ownerId, Instant startsAt, Instant endsAt, SlotStatus status,
        long version) {
}
