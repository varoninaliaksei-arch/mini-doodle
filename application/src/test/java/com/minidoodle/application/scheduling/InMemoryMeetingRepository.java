package com.minidoodle.application.scheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.Meeting;

/** Fake repository port — no Spring context, no database. */
class InMemoryMeetingRepository implements MeetingRepository {

    private final Map<UUID, Meeting> store = new HashMap<>();
    private RuntimeException nextSaveException;

    void seed(Meeting meeting) {
        store.put(meeting.id(), meeting);
    }

    void failNextSaveWith(RuntimeException exception) {
        this.nextSaveException = exception;
    }

    @Override
    public Optional<Meeting> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Meeting> findByIdempotencyKey(String idempotencyKey) {
        return store.values().stream()
                .filter(m -> idempotencyKey.equals(m.idempotencyKey()))
                .findFirst();
    }

    @Override
    public List<Meeting> findBySlotId(UUID slotId) {
        return store.values().stream().filter(m -> slotId.equals(m.slotId())).toList();
    }

    @Override
    public Meeting save(Meeting meeting) {
        if (nextSaveException != null) {
            RuntimeException toThrow = nextSaveException;
            nextSaveException = null;
            throw toThrow;
        }
        store.put(meeting.id(), meeting);
        return meeting;
    }
}
