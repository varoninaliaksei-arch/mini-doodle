package com.minidoodle.domain.scheduling;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class SlotStatusTest {

    @Test
    void hasExactlyThreeValues() {
        assertArrayEquals(
                new SlotStatus[] {SlotStatus.FREE, SlotStatus.BLOCKED, SlotStatus.BOOKED},
                SlotStatus.values());
    }

    @Test
    void valueOfRoundTrips() {
        for (SlotStatus status : SlotStatus.values()) {
            assertEquals(status, SlotStatus.valueOf(status.name()));
        }
    }
}
