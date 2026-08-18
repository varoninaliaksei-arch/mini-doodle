package com.minidoodle.application.scheduling;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

/** Fake repository port — no Spring context, no database. */
class InMemoryTimeSlotRepository implements TimeSlotRepository {

    private final Map<UUID, TimeSlot> store = new HashMap<>();
    private RuntimeException nextSaveException;
    private RuntimeException nextDeleteException;

    void seed(TimeSlot slot) {
        store.put(slot.id(), slot);
    }

    void failNextSaveWith(RuntimeException exception) {
        this.nextSaveException = exception;
    }

    void failNextDeleteWith(RuntimeException exception) {
        this.nextDeleteException = exception;
    }

    boolean contains(UUID id) {
        return store.containsKey(id);
    }

    @Override
    public Optional<TimeSlot> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public TimeSlot save(TimeSlot slot) {
        if (nextSaveException != null) {
            RuntimeException toThrow = nextSaveException;
            nextSaveException = null;
            throw toThrow;
        }
        store.put(slot.id(), slot);
        return slot;
    }

    @Override
    public void deleteById(UUID id) {
        if (nextDeleteException != null) {
            RuntimeException toThrow = nextDeleteException;
            nextDeleteException = null;
            throw toThrow;
        }
        store.remove(id);
    }

    @Override
    public List<TimeSlot> findByCalendarAndWindow(UUID calendarId, TimeInterval window) {
        return store.values().stream()
                .filter(s -> s.calendarId().equals(calendarId) && s.interval().overlaps(window))
                .toList();
    }
}
