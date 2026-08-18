package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {

    void save(OutboxEvent event);

    /**
     * Unpublished rows, oldest first, capped at {@code limit}. Implementors
     * must use {@code FOR UPDATE SKIP LOCKED} so concurrent
     * {@code OutboxPublisher} instances (scale profile, running more than
     * one replica) don't claim the same batch.
     */
    List<OutboxEvent> findUnpublished(int limit);

    void markPublished(UUID id);
}
