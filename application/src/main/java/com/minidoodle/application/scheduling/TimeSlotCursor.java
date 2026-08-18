package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Decoded keyset-pagination position for GET /slots. The Base64 wire
 * format is an infrastructure-only concern (encode/decode happens in
 * infrastructure/.../web) — this type only ever carries the
 * already-decoded {@code (starts_at, id)} pair.
 */
public record TimeSlotCursor(Instant startsAt, UUID id) {

    public TimeSlotCursor {
        Objects.requireNonNull(startsAt, "startsAt must not be null");
        Objects.requireNonNull(id, "id must not be null");
    }
}
