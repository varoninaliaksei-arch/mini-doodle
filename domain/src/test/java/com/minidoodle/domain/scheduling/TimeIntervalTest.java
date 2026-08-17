package com.minidoodle.domain.scheduling;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeIntervalTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-01T01:00:00Z");
    private static final Instant T2 = Instant.parse("2026-01-01T02:00:00Z");
    private static final Instant T3 = Instant.parse("2026-01-01T03:00:00Z");

    @Test
    void overlappingIntervalsOverlap() {
        TimeInterval a = new TimeInterval(T0, T2);
        TimeInterval b = new TimeInterval(T1, T3);

        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
    }

    @Test
    void disjointIntervalsDoNotOverlap() {
        TimeInterval a = new TimeInterval(T0, T1);
        TimeInterval b = new TimeInterval(T2, T3);

        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
    }

    @Test
    void identicalIntervalsOverlap() {
        TimeInterval a = new TimeInterval(T0, T2);
        TimeInterval b = new TimeInterval(T0, T2);

        assertTrue(a.overlaps(b));
    }

    @Test
    void edgeTouchingIntervalsAreAdjacentButNotOverlapping() {
        // a = [T0, T1), b = [T1, T2) — a.end == b.start
        TimeInterval a = new TimeInterval(T0, T1);
        TimeInterval b = new TimeInterval(T1, T2);

        assertFalse(a.overlaps(b));
        assertFalse(b.overlaps(a));
        assertTrue(a.isAdjacentOrOverlapping(b));
        assertTrue(b.isAdjacentOrOverlapping(a));
    }

    @Test
    void nonAdjacentDisjointIntervalsAreNotAdjacentOrOverlapping() {
        TimeInterval a = new TimeInterval(T0, T1);
        TimeInterval b = new TimeInterval(T2, T3);

        assertFalse(a.isAdjacentOrOverlapping(b));
        assertFalse(b.isAdjacentOrOverlapping(a));
    }

    @Test
    void overlappingIntervalsAreAlsoAdjacentOrOverlapping() {
        TimeInterval a = new TimeInterval(T0, T2);
        TimeInterval b = new TimeInterval(T1, T3);

        assertTrue(a.isAdjacentOrOverlapping(b));
        assertTrue(b.isAdjacentOrOverlapping(a));
    }

    @Test
    void constructorRejectsStartEqualToEnd() {
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(T0, T0));
    }

    @Test
    void constructorRejectsStartAfterEnd() {
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(T1, T0));
    }

    @Test
    void constructorRejectsNulls() {
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(null, T1));
        assertThrows(IllegalArgumentException.class, () -> new TimeInterval(T0, null));
    }

    @Test
    void accessorsReturnConstructorArguments() {
        TimeInterval interval = new TimeInterval(T0, T1);

        assertEquals(T0, interval.start());
        assertEquals(T1, interval.end());
    }
}
