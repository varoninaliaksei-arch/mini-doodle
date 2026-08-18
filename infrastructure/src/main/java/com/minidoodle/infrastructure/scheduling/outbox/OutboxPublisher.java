package com.minidoodle.infrastructure.scheduling.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import com.minidoodle.application.scheduling.OutboxEvent;
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
 *
 * <p>Also owns {@code outbox.lag} (§11, SCOPE-3): a pull-based gauge reading
 * {@link #lagSeconds}, a field this class refreshes on every {@link #drain()}
 * run rather than the gauge computing it directly — the query result
 * (oldest-first, per the repository's contract) is already in hand from
 * draining, so there is no reason for the gauge's scrape-time read to issue
 * a second query.
 */
@Component
class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final PublishOutboxEventUseCase publishOutboxEventUseCase;
    private final int batchSize;

    private volatile double lagSeconds;

    OutboxPublisher(OutboxEventRepository outboxEventRepository, PublishOutboxEventUseCase publishOutboxEventUseCase,
            @Value("${outbox.publisher.batch-size}") int batchSize, MeterRegistry meterRegistry) {
        this.outboxEventRepository = outboxEventRepository;
        this.publishOutboxEventUseCase = publishOutboxEventUseCase;
        this.batchSize = batchSize;
        Gauge.builder("outbox.lag", this, OutboxPublisher::lagSeconds)
                .description("Age in seconds of the oldest unpublished outbox event")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay}")
    void drain() {
        List<OutboxEvent> unpublished = outboxEventRepository.findUnpublished(batchSize);
        lagSeconds = unpublished.isEmpty() ? 0
                : Duration.between(unpublished.get(0).createdAt(), Instant.now()).toSeconds();
        unpublished.forEach(publishOutboxEventUseCase::execute);
    }

    private double lagSeconds() {
        return lagSeconds;
    }
}
