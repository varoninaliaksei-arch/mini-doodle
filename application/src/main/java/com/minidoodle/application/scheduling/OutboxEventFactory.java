package com.minidoodle.application.scheduling;

import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.domain.scheduling.events.MeetingEvent;

/**
 * Placeholder serialization (plain JSON via Jackson) — finalized when the
 * outbox publisher is implemented. Jackson 3's writeValueAsString throws
 * JacksonException, which is unchecked (unlike Jackson 2's checked
 * JsonProcessingException) — no wrapping needed.
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
