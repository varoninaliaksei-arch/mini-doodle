package com.minidoodle.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.minidoodle.domain.scheduling.SlotStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "time_slots")
public class TimeSlotJpaEntity {

    @Id
    private UUID id;

    @Column(name = "calendar_id", nullable = false)
    private UUID calendarId;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SlotStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected TimeSlotJpaEntity() {
        // JPA
    }

    public TimeSlotJpaEntity(UUID id, UUID calendarId, Instant startsAt, Instant endsAt, SlotStatus status,
            long version) {
        this.id = id;
        this.calendarId = calendarId;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = status;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCalendarId() {
        return calendarId;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }
}
