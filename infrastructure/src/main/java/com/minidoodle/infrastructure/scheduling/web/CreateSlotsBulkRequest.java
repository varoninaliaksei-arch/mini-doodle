package com.minidoodle.infrastructure.scheduling.web;

import java.time.Duration;
import java.time.Instant;

public record CreateSlotsBulkRequest(Instant startsAt, Instant endsAt, Duration slotDuration) {
}
