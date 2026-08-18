package com.minidoodle.infrastructure.scheduling.outbox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.minidoodle.application.scheduling.EventSink;
import com.minidoodle.application.scheduling.OutboxEvent;
import com.minidoodle.application.scheduling.OutboxEventRepository;
import com.minidoodle.application.scheduling.PublishOutboxEventUseCase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** No Spring context — drain() is called directly, no @Scheduled trigger involved. */
class OutboxPublisherTest {

    private final FakeOutboxEventRepository repository = new FakeOutboxEventRepository();
    private final RecordingEventSink sink = new RecordingEventSink();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final OutboxPublisher publisher = new OutboxPublisher(repository,
            new PublishOutboxEventUseCase(repository, sink), 50, meterRegistry);

    @Test
    void publishesUnpublishedRowsAndMarksThemPublished() {
        OutboxEvent event = anEvent(Instant.now());
        repository.save(event);

        publisher.drain();

        assertEquals(List.of(event), sink.published);
        assertTrue(repository.isPublished(event.id()));
    }

    @Test
    void doesNotTouchAlreadyPublishedRows() {
        OutboxEvent alreadyPublished = anEvent(Instant.now());
        repository.save(alreadyPublished);
        repository.markPublished(alreadyPublished.id());

        publisher.drain();

        assertTrue(sink.published.isEmpty());
    }

    @Test
    void gaugeReflectsAgeOfTheOldestUnpublishedRow() {
        repository.save(anEvent(Instant.now().minusSeconds(300)));
        repository.save(anEvent(Instant.now().minusSeconds(120)));

        publisher.drain();

        double lag = meterRegistry.get("outbox.lag").gauge().value();
        assertTrue(lag >= 300, () -> "expected lag to reflect the oldest row (~300s), got " + lag);
    }

    @Test
    void gaugeReadsZeroWhenNothingIsPending() {
        publisher.drain();

        double lag = meterRegistry.get("outbox.lag").gauge().value();
        assertEquals(0.0, lag);
    }

    private static OutboxEvent anEvent(Instant createdAt) {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "MeetingBooked", "{}", createdAt);
    }

    private static final class FakeOutboxEventRepository implements OutboxEventRepository {

        private final List<OutboxEvent> events = new ArrayList<>();
        private final List<UUID> published = new ArrayList<>();

        @Override
        public void save(OutboxEvent event) {
            events.add(event);
        }

        @Override
        public List<OutboxEvent> findUnpublished(int limit) {
            return events.stream()
                    .filter(event -> !published.contains(event.id()))
                    .sorted(Comparator.comparing(OutboxEvent::createdAt))
                    .limit(limit)
                    .toList();
        }

        @Override
        public void markPublished(UUID id) {
            published.add(id);
        }

        boolean isPublished(UUID id) {
            return published.contains(id);
        }
    }

    private static final class RecordingEventSink implements EventSink {

        private final List<OutboxEvent> published = new ArrayList<>();

        @Override
        public void publish(OutboxEvent event) {
            published.add(event);
        }
    }
}
