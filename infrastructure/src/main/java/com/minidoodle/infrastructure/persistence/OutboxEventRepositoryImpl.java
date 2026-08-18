package com.minidoodle.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.minidoodle.application.scheduling.OutboxEvent;
import com.minidoodle.application.scheduling.OutboxEventRepository;

@Repository
class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventJpaRepository jpaRepository;

    OutboxEventRepositoryImpl(OutboxEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(OutboxEvent event) {
        jpaRepository.save(new OutboxEventJpaEntity(event.id(), event.aggregateId(), event.type(), event.payload()));
    }

    @Override
    public List<OutboxEvent> findUnpublished(int limit) {
        return jpaRepository.findUnpublishedForUpdateSkipLocked(limit).stream()
                .map(entity -> new OutboxEvent(entity.getId(), entity.getAggregateId(), entity.getType(),
                        entity.getPayload(), entity.getCreatedAt()))
                .toList();
    }

    @Override
    public void markPublished(UUID id) {
        OutboxEventJpaEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("outbox event not found: " + id));
        entity.markPublished(Instant.now());
        jpaRepository.save(entity);
    }
}
