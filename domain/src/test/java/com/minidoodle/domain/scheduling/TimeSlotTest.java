package com.minidoodle.domain.scheduling;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeSlotTest {

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));

    private static TimeSlot slotWith(SlotStatus status) {
        return new TimeSlot(UUID.randomUUID(), UUID.randomUUID(), INTERVAL, status, 0L);
    }

    // --- block(): FREE -> BLOCKED ---

    @Test
    void block_fromFree_transitionsToBlocked() {
        TimeSlot slot = slotWith(SlotStatus.FREE);

        slot.block();

        assertEquals(SlotStatus.BLOCKED, slot.status());
    }

    @Test
    void block_fromBlocked_throws() {
        TimeSlot slot = slotWith(SlotStatus.BLOCKED);

        assertThrows(IllegalStateException.class, slot::block);
        assertEquals(SlotStatus.BLOCKED, slot.status());
    }

    @Test
    void block_fromBooked_throws_forbiddenDirectTransition() {
        // DOM-1: BOOKED -> BLOCKED is forbidden directly.
        TimeSlot slot = slotWith(SlotStatus.BOOKED);

        assertThrows(IllegalStateException.class, slot::block);
        assertEquals(SlotStatus.BOOKED, slot.status());
    }

    // --- unblock(): BLOCKED -> FREE ---

    @Test
    void unblock_fromBlocked_transitionsToFree() {
        TimeSlot slot = slotWith(SlotStatus.BLOCKED);

        slot.unblock();

        assertEquals(SlotStatus.FREE, slot.status());
    }

    @Test
    void unblock_fromFree_throws() {
        TimeSlot slot = slotWith(SlotStatus.FREE);

        assertThrows(IllegalStateException.class, slot::unblock);
        assertEquals(SlotStatus.FREE, slot.status());
    }

    @Test
    void unblock_fromBooked_throws() {
        TimeSlot slot = slotWith(SlotStatus.BOOKED);

        assertThrows(IllegalStateException.class, slot::unblock);
        assertEquals(SlotStatus.BOOKED, slot.status());
    }

    // --- book(): FREE -> BOOKED ---

    @Test
    void book_fromFree_transitionsToBooked() {
        TimeSlot slot = slotWith(SlotStatus.FREE);

        slot.book();

        assertEquals(SlotStatus.BOOKED, slot.status());
    }

    @Test
    void book_fromBlocked_throws() {
        TimeSlot slot = slotWith(SlotStatus.BLOCKED);

        assertThrows(IllegalStateException.class, slot::book);
        assertEquals(SlotStatus.BLOCKED, slot.status());
    }

    @Test
    void book_fromBooked_throws_reentrantCall() {
        // Re-entrant book() on an already-BOOKED slot must not succeed silently.
        TimeSlot slot = slotWith(SlotStatus.BOOKED);

        assertThrows(IllegalStateException.class, slot::book);
        assertEquals(SlotStatus.BOOKED, slot.status());
    }

    // --- release(): BOOKED -> FREE ---

    @Test
    void release_fromBooked_transitionsToFree() {
        TimeSlot slot = slotWith(SlotStatus.BOOKED);

        slot.release();

        assertEquals(SlotStatus.FREE, slot.status());
    }

    @Test
    void release_fromFree_throws() {
        TimeSlot slot = slotWith(SlotStatus.FREE);

        assertThrows(IllegalStateException.class, slot::release);
        assertEquals(SlotStatus.FREE, slot.status());
    }

    @Test
    void release_fromBlocked_throws() {
        TimeSlot slot = slotWith(SlotStatus.BLOCKED);

        assertThrows(IllegalStateException.class, slot::release);
        assertEquals(SlotStatus.BLOCKED, slot.status());
    }

    // --- identity, factory, invariants ---

    @Test
    void freeFactoryCreatesFreeSlotAtVersionZero() {
        UUID id = UUID.randomUUID();
        UUID calendarId = UUID.randomUUID();

        TimeSlot slot = TimeSlot.free(id, calendarId, INTERVAL);

        assertEquals(id, slot.id());
        assertEquals(calendarId, slot.calendarId());
        assertEquals(INTERVAL, slot.interval());
        assertEquals(SlotStatus.FREE, slot.status());
        assertEquals(0L, slot.version());
    }

    @Test
    void equalityIsBasedOnIdentityNotState() {
        UUID id = UUID.randomUUID();
        TimeSlot free = new TimeSlot(id, UUID.randomUUID(), INTERVAL, SlotStatus.FREE, 0L);
        TimeSlot bookedSameId = new TimeSlot(id, UUID.randomUUID(), INTERVAL, SlotStatus.BOOKED, 5L);
        TimeSlot differentId = slotWith(SlotStatus.FREE);

        assertEquals(free, bookedSameId);
        assertEquals(free.hashCode(), bookedSameId.hashCode());
        assertNotEquals(free, differentId);
    }
}
