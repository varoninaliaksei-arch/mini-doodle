package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A row appended to / read back from {@code outbox_events}. Not a
 * domain concept. {@code createdAt} is null for an event not yet persisted
 * (the DB's {@code DEFAULT now()} is the source of truth) and
 * populated only when read back via {@link OutboxEventRepository#findUnpublished},
 * where it drives the {@code outbox.lag} gauge.
 */
public record OutboxEvent(UUID id, UUID aggregateId, String type, String payload, Instant createdAt) {

    public OutboxEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("payload must not be blank");
        }
    }
}
