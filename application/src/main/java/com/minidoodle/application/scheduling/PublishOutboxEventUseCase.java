package com.minidoodle.application.scheduling;

import org.springframework.transaction.annotation.Transactional;

/**
 * Publish-then-mark for exactly one {@code outbox_events} row. Called
 * once per event from {@code OutboxPublisher}'s batch loop in
 * infrastructure — a separate Spring bean, so each call runs in its own
 * transaction (self-invocation from within the loop's own class would
 * bypass the @Transactional proxy). That per-call boundary keeps a
 * failure partway through a batch from rolling back rows already
 * committed earlier in the same run.
 */
public class PublishOutboxEventUseCase {

    private final OutboxEventRepository outboxEventRepository;
    private final EventSink eventSink;

    public PublishOutboxEventUseCase(OutboxEventRepository outboxEventRepository, EventSink eventSink) {
        this.outboxEventRepository = outboxEventRepository;
        this.eventSink = eventSink;
    }

    @Transactional
    public void execute(OutboxEvent event) {
        eventSink.publish(event);
        outboxEventRepository.markPublished(event.id());
    }
}
