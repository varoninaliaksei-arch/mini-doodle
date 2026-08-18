package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Objects;

import com.minidoodle.domain.scheduling.TimeSlot;

public record SlotPage(List<TimeSlot> items, boolean hasMore) {

    public SlotPage {
        items = List.copyOf(Objects.requireNonNull(items, "items must not be null"));
    }
}
