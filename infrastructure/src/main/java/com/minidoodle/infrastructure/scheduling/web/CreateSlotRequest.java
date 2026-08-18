package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;

public record CreateSlotRequest(Instant startsAt, Instant endsAt) {
}
