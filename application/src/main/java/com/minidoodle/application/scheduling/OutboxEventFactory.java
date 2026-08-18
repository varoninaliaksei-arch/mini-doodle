package com.minidoodle.application.scheduling;

import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.domain.scheduling.events.MeetingEvent;

/**
 * Placeholder serialization (plain JSON via Jackson) — finalized when the
 * outbox publisher is implemented.
 */
class OutboxEventFactory {

    private final ObjectMapper objectMapper;

    OutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    OutboxEvent from(MeetingEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            return new OutboxEvent(UUID.randomUUID(), event.meetingId(), event.getClass().getSimpleName(), payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + event.getClass().getSimpleName(), e);
        }
    }
}
