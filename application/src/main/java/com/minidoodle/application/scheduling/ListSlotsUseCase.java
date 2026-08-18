package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

/** GET /slots: keyset-paginated slot listing, TECH-7 window cap enforced. */
public class ListSlotsUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public ListSlotsUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public SlotPage execute(UUID calendarId, TimeInterval window, Optional<SlotStatus> status,
            Optional<TimeSlotCursor> after, int limit) {
        WindowPolicy.enforce(window);

        List<TimeSlot> overFetched = timeSlotRepository.findPage(calendarId, window, status, after, limit + 1);
        boolean hasMore = overFetched.size() > limit;
        List<TimeSlot> page = hasMore ? overFetched.subList(0, limit) : overFetched;
        return new SlotPage(page, hasMore);
    }
}
