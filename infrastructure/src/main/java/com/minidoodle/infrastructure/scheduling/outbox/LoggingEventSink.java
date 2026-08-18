package com.minidoodle.infrastructure.scheduling.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.minidoodle.application.scheduling.EventSink;
import com.minidoodle.application.scheduling.OutboxEvent;

/**
 * Placeholder publish step (§8): logs the event at INFO, structured enough
 * to grep. Swapping in a {@code KafkaTemplate} later touches only this
 * class.
 */
@Component
class LoggingEventSink implements EventSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventSink.class);

    @Override
    public void publish(OutboxEvent event) {
        log.info("outbox.publish id={} type={} aggregateId={} payload={}",
                event.id(), event.type(), event.aggregateId(), event.payload());
    }
}
