package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

public interface TimeSlotRepository {

    Optional<TimeSlot> findById(UUID id);

    TimeSlot save(TimeSlot slot);

    void deleteById(UUID id);

    List<TimeSlot> findByCalendarAndWindow(UUID calendarId, TimeInterval window);

    /**
     * Keyset page ordered by {@code (starts_at, id)} — matches the B-tree
     * index backing GET /slots pagination.
     * Returns at most {@code limit} slots strictly after {@code after}
     * (or from the start of the window if {@code after} is empty).
     */
    List<TimeSlot> findPage(UUID calendarId, TimeInterval window, Optional<SlotStatus> status,
            Optional<TimeSlotCursor> after, int limit);
}
