package com.minidoodle.domain.scheduling;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AvailabilityAggregatorTest {

    private final UUID calendarId = UUID.randomUUID();
    private final Instant windowStart = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant windowEnd = Instant.parse("2026-01-01T10:00:00Z");
    private final TimeInterval window = new TimeInterval(windowStart, windowEnd);

    @Test
    void emptySlotListIsEntirelyUnavailable() {
        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of());

        assertEquals(List.of(new CoverageInterval(window, Coverage.UNAVAILABLE)), result);
    }

    @Test
    void singleSlotFullyCoveringWindowProducesOneInterval() {
        TimeSlot slot = slot("00:00:00Z", "10:00:00Z", SlotStatus.FREE);

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of(slot));

        assertEquals(List.of(new CoverageInterval(window, Coverage.FREE)), result);
    }

    @Test
    void gapAtStartOfWindowIsUnavailable() {
        TimeSlot slot = slot("02:00:00Z", "10:00:00Z", SlotStatus.FREE);

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of(slot));

        assertEquals(List.of(
                new CoverageInterval(interval("00:00:00Z", "02:00:00Z"), Coverage.UNAVAILABLE),
                new CoverageInterval(interval("02:00:00Z", "10:00:00Z"), Coverage.FREE)),
                result);
    }

    @Test
    void gapAtEndOfWindowIsUnavailable() {
        TimeSlot slot = slot("00:00:00Z", "08:00:00Z", SlotStatus.FREE);

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of(slot));

        assertEquals(List.of(
                new CoverageInterval(interval("00:00:00Z", "08:00:00Z"), Coverage.FREE),
                new CoverageInterval(interval("08:00:00Z", "10:00:00Z"), Coverage.UNAVAILABLE)),
                result);
    }

    @Test
    void gapInMiddleBetweenTwoSlotsIsUnavailable() {
        TimeSlot first = slot("00:00:00Z", "02:00:00Z", SlotStatus.FREE);
        TimeSlot second = slot("05:00:00Z", "10:00:00Z", SlotStatus.BOOKED);

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of(first, second));

        assertEquals(List.of(
                new CoverageInterval(interval("00:00:00Z", "02:00:00Z"), Coverage.FREE),
                new CoverageInterval(interval("02:00:00Z", "05:00:00Z"), Coverage.UNAVAILABLE),
                new CoverageInterval(interval("05:00:00Z", "10:00:00Z"), Coverage.BUSY)),
                result);
    }

    @Test
    void adjacentSlotsWithSameStatusAreMerged() {
        TimeSlot first = slot("00:00:00Z", "02:00:00Z", SlotStatus.FREE);
        TimeSlot second = slot("02:00:00Z", "05:00:00Z", SlotStatus.FREE);
        TimeInterval narrowWindow = interval("00:00:00Z", "05:00:00Z");

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(narrowWindow, List.of(first, second));

        assertEquals(List.of(new CoverageInterval(narrowWindow, Coverage.FREE)), result);
    }

    @Test
    void adjacentSlotsWithDifferentStatusAreNotMerged() {
        TimeSlot first = slot("00:00:00Z", "02:00:00Z", SlotStatus.FREE);
        TimeSlot second = slot("02:00:00Z", "05:00:00Z", SlotStatus.BOOKED);
        TimeInterval narrowWindow = interval("00:00:00Z", "05:00:00Z");

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(narrowWindow, List.of(first, second));

        assertEquals(List.of(
                new CoverageInterval(interval("00:00:00Z", "02:00:00Z"), Coverage.FREE),
                new CoverageInterval(interval("02:00:00Z", "05:00:00Z"), Coverage.BUSY)),
                result);
    }

    @Test
    void slotPartiallyOutsideWindowIsClampedToWindowBounds() {
        Instant beforeWindow = windowStart.minusSeconds(3600);
        Instant afterWindow = windowEnd.plusSeconds(3600);
        Instant boundary = Instant.parse("2026-01-01T03:00:00Z");

        TimeSlot leading = new TimeSlot(UUID.randomUUID(), calendarId,
                new TimeInterval(beforeWindow, boundary), SlotStatus.FREE, 0L);
        TimeSlot trailing = new TimeSlot(UUID.randomUUID(), calendarId,
                new TimeInterval(boundary, afterWindow), SlotStatus.BOOKED, 0L);

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(window, List.of(leading, trailing));

        assertEquals(List.of(
                new CoverageInterval(interval("00:00:00Z", "03:00:00Z"), Coverage.FREE),
                new CoverageInterval(interval("03:00:00Z", "10:00:00Z"), Coverage.BUSY)),
                result);
    }

    @Test
    void blockedAndBookedBothMapToBusy() {
        TimeSlot blocked = slot("00:00:00Z", "02:00:00Z", SlotStatus.BLOCKED);
        TimeSlot booked = slot("02:00:00Z", "04:00:00Z", SlotStatus.BOOKED);
        TimeInterval narrowWindow = interval("00:00:00Z", "04:00:00Z");

        List<CoverageInterval> result = AvailabilityAggregator.aggregate(narrowWindow, List.of(blocked, booked));

        assertEquals(List.of(new CoverageInterval(narrowWindow, Coverage.BUSY)), result);
    }

    private TimeSlot slot(String start, String end, SlotStatus status) {
        return new TimeSlot(UUID.randomUUID(), calendarId, interval(start, end), status, 0L);
    }

    private TimeInterval interval(String start, String end) {
        return new TimeInterval(Instant.parse("2026-01-01T" + start), Instant.parse("2026-01-01T" + end));
    }
}
