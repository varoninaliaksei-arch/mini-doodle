package com.minidoodle.domain.scheduling;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root. Status transitions are guarded here (DOM-1); invalid
 * transitions throw {@link IllegalStateException}, which the application
 * layer maps to {@code 409}.
 */
public final class TimeSlot {

    private final UUID id;
    private final UUID calendarId;
    private final TimeInterval interval;
    private SlotStatus status;
    private long version;

    public TimeSlot(UUID id, UUID calendarId, TimeInterval interval, SlotStatus status, long version) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.calendarId = Objects.requireNonNull(calendarId, "calendarId must not be null");
        this.interval = Objects.requireNonNull(interval, "interval must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.version = version;
    }

    public static TimeSlot free(UUID id, UUID calendarId, TimeInterval interval) {
        return new TimeSlot(id, calendarId, interval, SlotStatus.FREE, 0L);
    }

    public void block() {
        requireStatus(SlotStatus.FREE, "block");
        status = SlotStatus.BLOCKED;
    }

    public void unblock() {
        requireStatus(SlotStatus.BLOCKED, "unblock");
        status = SlotStatus.FREE;
    }

    public void book() {
        requireStatus(SlotStatus.FREE, "book");
        status = SlotStatus.BOOKED;
    }

    /**
     * Contract: call only from the meeting-cancellation use case. Never call
     * this from slot delete/modify — a booked slot must be released through
     * cancelling its meeting, per DOM-2.
     */
    public void release() {
        requireStatus(SlotStatus.BOOKED, "release");
        status = SlotStatus.FREE;
    }

    private void requireStatus(SlotStatus required, String operation) {
        if (status != required) {
            throw new IllegalStateException(
                    "Cannot %s slot %s: expected status %s but was %s"
                            .formatted(operation, id, required, status));
        }
    }

    public UUID id() {
        return id;
    }

    public UUID calendarId() {
        return calendarId;
    }

    public TimeInterval interval() {
        return interval;
    }

    public SlotStatus status() {
        return status;
    }

    public long version() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeSlot other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
