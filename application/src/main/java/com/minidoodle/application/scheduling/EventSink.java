package com.minidoodle.application.scheduling;

/**
 * Publish hook for one {@code outbox_events} row (§8, INFRA-3). The only
 * implementation today logs; swapping in a {@code KafkaTemplate} later
 * touches only the infrastructure adapter, not this port or the
 * booking/cancellation write path.
 */
public interface EventSink {

    void publish(OutboxEvent event);
}
