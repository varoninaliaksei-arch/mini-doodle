package com.minidoodle.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

class TimeSlotMapperTest {

    private final TimeSlotMapper mapper = new TimeSlotMapper();

    @Test
    void roundTripsThroughEntity() {
        TimeSlot original = new TimeSlot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new TimeInterval(Instant.parse("2026-01-01T09:00:00Z"), Instant.parse("2026-01-01T09:30:00Z")),
                SlotStatus.BOOKED,
                3L);

        TimeSlotJpaEntity entity = mapper.toEntity(original);
        TimeSlot roundTripped = mapper.toDomain(entity);

        assertEquals(original.id(), roundTripped.id());
        assertEquals(original.calendarId(), roundTripped.calendarId());
        assertEquals(original.interval(), roundTripped.interval());
        assertEquals(original.status(), roundTripped.status());
        assertEquals(original.version(), roundTripped.version());
    }
}
