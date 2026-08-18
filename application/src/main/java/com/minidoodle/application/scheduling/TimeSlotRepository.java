package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

public interface TimeSlotRepository {

    Optional<TimeSlot> findById(UUID id);

    TimeSlot save(TimeSlot slot);

    List<TimeSlot> findByCalendarAndWindow(UUID calendarId, TimeInterval window);
}
