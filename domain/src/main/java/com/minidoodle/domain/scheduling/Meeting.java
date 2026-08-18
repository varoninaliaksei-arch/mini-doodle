package com.minidoodle.domain.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.minidoodle.domain.scheduling.events.MeetingBooked;
import com.minidoodle.domain.scheduling.events.MeetingCancelled;
import com.minidoodle.domain.scheduling.events.MeetingEvent;

/**
 * Aggregate root. schedule()/cancel() never touch TimeSlot or any
 * repository — cross-aggregate orchestration (slot.book()/slot.release()
 * alongside this aggregate, idempotency-key lookup, persisting both
 * aggregates and writing to outbox_events) belongs only in the application
 * layer, one @Transactional use case method per operation.
 */
public final class Meeting {

    private final UUID id;
    private final UUID slotId;
    private final MeetingDetails details;
    private final List<Participant> participants;
    private final String idempotencyKey;
    private MeetingStatus status;
    private final List<MeetingEvent> events = new ArrayList<>();

    public Meeting(UUID id, UUID slotId, MeetingDetails details, List<Participant> participants,
            MeetingStatus status, String idempotencyKey) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.slotId = Objects.requireNonNull(slotId, "slotId must not be null");
        this.details = Objects.requireNonNull(details, "details must not be null");
        this.participants = List.copyOf(Objects.requireNonNull(participants, "participants must not be null"));
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.idempotencyKey = normalizeIdempotencyKey(idempotencyKey);
    }

    public static Meeting schedule(UUID slotId, MeetingDetails details, List<Participant> participants,
            String idempotencyKey) {
        Meeting meeting = new Meeting(UUID.randomUUID(), slotId, details, participants,
                MeetingStatus.SCHEDULED, idempotencyKey);
        meeting.events.add(new MeetingBooked(meeting.id, meeting.slotId));
        return meeting;
    }

    public void cancel() {
        requireStatus(MeetingStatus.SCHEDULED, "cancel");
        status = MeetingStatus.CANCELLED;
        events.add(new MeetingCancelled(id, slotId));
    }

    public List<MeetingEvent> pullEvents() {
        List<MeetingEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }

    private void requireStatus(MeetingStatus required, String operation) {
        if (status != required) {
            throw new IllegalStateException(
                    "Cannot %s meeting %s: expected status %s but was %s"
                            .formatted(operation, id, required, status));
        }
    }

    /**
     * Idempotency-Key is an optional HTTP header, so {@code null}
     * is a valid value here (no key supplied). A supplied-but-blank string
     * is rejected as a caller error.
     */
    private static String normalizeIdempotencyKey(String value) {
        if (value != null && value.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank when provided");
        }
        return value;
    }

    public UUID id() {
        return id;
    }

    public UUID slotId() {
        return slotId;
    }

    public MeetingDetails details() {
        return details;
    }

    public List<Participant> participants() {
        return participants;
    }

    public MeetingStatus status() {
        return status;
    }

    public String idempotencyKey() {
        return idempotencyKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Meeting other)) {
            return false;
        }
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
