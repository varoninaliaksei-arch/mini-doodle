package com.minidoodle.infrastructure.scheduling.outbox;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.minidoodle.application.scheduling.OutboxEventRepository;
import com.minidoodle.application.scheduling.PublishOutboxEventUseCase;

/**
 * Drains {@code outbox_events} (02-ARCHITECTURE.md §8, INFRA-3): fetches a
 * small unpublished batch and publishes each row through
 * {@link PublishOutboxEventUseCase} — a separate Spring bean, so each call
 * runs in its own transaction rather than being bypassed by self-invocation.
 * If one row's processing throws, rows already published earlier in this
 * run stay committed (each was its own transaction); the loop stops for
 * this tick and the next {@code fixedDelay} run picks up where it left off.
 */
@Component
class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PublishOutboxEventUseCase publishOutboxEventUseCase;
    private final int batchSize;

    OutboxPublisher(OutboxEventRepository outboxEventRepository, PublishOutboxEventUseCase publishOutboxEventUseCase,
            @Value("${outbox.publisher.batch-size}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.publishOutboxEventUseCase = publishOutboxEventUseCase;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay}")
    void drain() {
        outboxEventRepository.findUnpublished(batchSize).forEach(publishOutboxEventUseCase::execute);
    }
}
