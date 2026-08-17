package com.minidoodle.domain.scheduling;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * DOM-4: aggregated free/busy view. Pure function, no Spring, no
 * repository access — operates on an already-loaded slot list. Gaps in the
 * window with no slot become UNAVAILABLE; adjacent intervals of the same
 * coverage are merged.
 */
public final class AvailabilityAggregator {

    private AvailabilityAggregator() {
    }

    /**
     * Assumes {@code slots} are non-overlapping and belong to a single
     * calendar — guaranteed by the DB exclusion constraint at write time.
     * Behavior is undefined if this precondition doesn't hold.
     */
    public static List<CoverageInterval> aggregate(TimeInterval window, List<TimeSlot> slots) {
        List<TimeSlot> sorted = slots.stream()
                .filter(slot -> slot.interval().overlaps(window))
                .sorted(Comparator.comparing(slot -> slot.interval().start()))
                .toList();

        List<CoverageInterval> raw = new ArrayList<>();
        Instant cursor = window.start();

        for (TimeSlot slot : sorted) {
            Instant slotStart = clamp(slot.interval().start(), window);
            Instant slotEnd = clamp(slot.interval().end(), window);

            if (cursor.isBefore(slotStart)) {
                raw.add(new CoverageInterval(new TimeInterval(cursor, slotStart), Coverage.UNAVAILABLE));
            }
            raw.add(new CoverageInterval(new TimeInterval(slotStart, slotEnd), toCoverage(slot.status())));
            cursor = slotEnd;
        }

        if (cursor.isBefore(window.end())) {
            raw.add(new CoverageInterval(new TimeInterval(cursor, window.end()), Coverage.UNAVAILABLE));
        }

        return merge(raw);
    }

    private static Instant clamp(Instant instant, TimeInterval window) {
        if (instant.isBefore(window.start())) {
            return window.start();
        }
        if (instant.isAfter(window.end())) {
            return window.end();
        }
        return instant;
    }

    private static Coverage toCoverage(SlotStatus status) {
        return switch (status) {
            case FREE -> Coverage.FREE;
            case BLOCKED, BOOKED -> Coverage.BUSY;
        };
    }

    private static List<CoverageInterval> merge(List<CoverageInterval> intervals) {
        List<CoverageInterval> merged = new ArrayList<>();
        for (CoverageInterval current : intervals) {
            if (!merged.isEmpty()) {
                CoverageInterval last = merged.getLast();
                if (last.coverage() == current.coverage()
                        && last.interval().end().equals(current.interval().start())) {
                    merged.set(merged.size() - 1, new CoverageInterval(
                            new TimeInterval(last.interval().start(), current.interval().end()), last.coverage()));
                    continue;
                }
            }
            merged.add(current);
        }
        return merged;
    }
}
