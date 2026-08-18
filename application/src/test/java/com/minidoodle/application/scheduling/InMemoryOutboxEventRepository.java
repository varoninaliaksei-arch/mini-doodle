package com.minidoodle.application.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fake repository port — no Spring context, no database. */
class InMemoryOutboxEventRepository implements OutboxEventRepository {

    final List<OutboxEvent> saved = new ArrayList<>();
    private final List<UUID> published = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
        saved.add(event);
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return saved.stream()
                .filter(event -> !published.contains(event.id()))
                .limit(limit)
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        published.add(id);
    }
}
