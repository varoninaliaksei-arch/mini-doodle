package com.minidoodle.infrastructure.persistence;

import org.springframework.stereotype.Component;

import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

@Component
public class TimeSlotMapper {

    public TimeSlot toDomain(TimeSlotJpaEntity entity) {
        return new TimeSlot(
                entity.getId(),
                entity.getCalendarId(),
                new TimeInterval(entity.getStartsAt(), entity.getEndsAt()),
                entity.getStatus(),
                entity.getVersion());
    }

    public TimeSlotJpaEntity toEntity(TimeSlot slot) {
        return new TimeSlotJpaEntity(
                slot.id(),
                slot.calendarId(),
                slot.interval().start(),
                slot.interval().end(),
                slot.status(),
                slot.version());
    }
}
