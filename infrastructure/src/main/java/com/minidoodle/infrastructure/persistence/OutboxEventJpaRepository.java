package com.minidoodle.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface OutboxEventJpaRepository extends JpaRepository<OutboxEventJpaEntity, UUID> {

    /**
     * {@code FOR UPDATE SKIP LOCKED} (native — JPQL has no equivalent) so
     * concurrent {@code OutboxPublisher} instances (scale profile, running
     * more than one replica) never wait on each other's in-flight batch: a
     * row already locked by another instance is simply skipped, not queued
     * behind. The lock is held only for this query's own short-lived
     * transaction (Spring Data wraps every repository method call in one
     * by default), so this is a best-effort exclusion rather than a hold
     * across the whole batch's processing — the crash-window duplicate
     * this creates remains the accepted at-least-once trade-off.
     */
    @Query(value = """
            select * from outbox_events
            where published_at is null
            order by created_at
            limit :limit
            for update skip locked
            """, nativeQuery = true)
    List<OutboxEventJpaEntity> findUnpublishedForUpdateSkipLocked(@Param("limit") int limit);
}
