package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.UUID;

import com.minidoodle.domain.scheduling.AvailabilityAggregator;
import com.minidoodle.domain.scheduling.CoverageInterval;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

/** GET /availability: merged free/busy/unavailable view, TECH-7 window cap enforced. */
public class GetAvailabilityUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public GetAvailabilityUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<CoverageInterval> execute(UUID calendarId, TimeInterval window) {
        WindowPolicy.enforce(window);

        List<TimeSlot> slots = timeSlotRepository.findByCalendarAndWindow(calendarId, window);
        return AvailabilityAggregator.aggregate(window, slots);
    }
}
