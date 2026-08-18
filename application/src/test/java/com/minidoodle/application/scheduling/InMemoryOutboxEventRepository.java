package com.minidoodle.application.scheduling;

import java.util.ArrayList;
import java.util.List;

/** Fake repository port — no Spring context, no database. */
class InMemoryOutboxEventRepository implements OutboxEventRepository {

    final List<OutboxEvent> saved = new ArrayList<>();

    @Override
    public void save(OutboxEvent event) {
        saved.add(event);
    }
}
