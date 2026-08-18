package com.minidoodle.application.scheduling;

import java.util.Objects;
import java.util.UUID;

/**
 * A row to append to {@code outbox_events} (INFRA-3). Not a domain concept —
 * {@code created_at}/{@code published_at} are populated by the database and
 * the outbox publisher respectively, neither of which exists yet.
 */
public record OutboxEvent(UUID id, UUID aggregateId, String type, String payload) {

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
