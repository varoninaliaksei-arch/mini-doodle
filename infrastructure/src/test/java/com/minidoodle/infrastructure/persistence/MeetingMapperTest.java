package com.minidoodle.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.MeetingStatus;
import com.minidoodle.domain.scheduling.Participant;

class MeetingMapperTest {

    private final MeetingMapper mapper = new MeetingMapper();

    @Test
    void roundTripsThroughEntity() {
        Meeting original = new Meeting(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new MeetingDetails("Design review", "Discuss the API contract", UUID.randomUUID()),
                List.of(
                        new Participant("alice@example.com", "Alice", UUID.randomUUID()),
                        new Participant("bob@example.com", "Bob", null)),
                MeetingStatus.SCHEDULED,
                "idem-key-1");

        MeetingJpaEntity entity = mapper.toEntity(original);
        Meeting roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.slotId(), roundTripped.slotId());
        assertEquals(original.details(), roundTripped.details());
        assertEquals(original.status(), roundTripped.status());
        assertEquals(original.idempotencyKey(), roundTripped.idempotencyKey());
        assertEquals(original.participants(), roundTripped.participants());
        assertTrue(roundTripped.pullEvents().isEmpty());
    }
}
