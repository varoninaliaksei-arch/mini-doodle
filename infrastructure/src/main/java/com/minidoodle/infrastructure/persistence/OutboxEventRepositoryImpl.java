package com.minidoodle.infrastructure.persistence;

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
}
