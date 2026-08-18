package com.minidoodle.application.scheduling;

import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.domain.scheduling.events.MeetingEvent;

/**
 * Serializes a {@link MeetingEvent} to the plain-JSON payload
 * {@code outbox_events} stores (§8, INFRA-3), read back and drained by
 * {@code OutboxPublisher}/{@code PublishOutboxEventUseCase}. Jackson 3's
 * {@code writeValueAsString} throws {@code JacksonException}, which is
 * unchecked (unlike Jackson 2's checked {@code JsonProcessingException}) —
 * no wrapping needed.
 */
class OutboxEventFactory {

    private final ObjectMapper objectMapper;

    OutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    OutboxEvent from(MeetingEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        return new OutboxEvent(UUID.randomUUID(), event.meetingId(), event.getClass().getSimpleName(), payload, null);
    }
}
