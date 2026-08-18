package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;

/** PATCH /slots/{id}: both fields optional, {@code null} means "unchanged". */
public record UpdateSlotRequest(Instant startsAt, Instant endsAt, long version) {
}
