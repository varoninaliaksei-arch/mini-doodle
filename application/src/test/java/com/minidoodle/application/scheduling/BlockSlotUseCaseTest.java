package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockSlotUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final BlockSlotUseCase useCase = new BlockSlotUseCase(repository);

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));

    @Test
    void blocksAFreeSlot() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);

        TimeSlot blocked = useCase.execute(slot.id(), ownerId, 0L);

        assertEquals(SlotStatus.BLOCKED, blocked.status());
    }

    @Test
    void rejectsBlockOfABookedSlotAndLeavesItInPlace() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.BOOKED, 0L);
        repository.seed(slot);

        assertThrows(SlotBookedException.class, () -> useCase.execute(slot.id(), ownerId, 0L));
        assertEquals(SlotStatus.BOOKED, repository.findById(slot.id()).orElseThrow().status());
    }

    @Test
    void rejectsStaleVersion() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 3L);
        repository.seed(slot);

        assertThrows(SlotVersionConflictException.class, () -> useCase.execute(slot.id(), ownerId, 2L));
    }

    @Test
    void slotNotFoundThrows() {
        assertThrows(SlotNotFoundException.class, () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID(), 0L));
    }

    @Test
    void rejectsCallerWhoIsNotTheOwner() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);

        assertThrows(NotOwnerException.class, () -> useCase.execute(slot.id(), UUID.randomUUID(), 0L));
        assertEquals(SlotStatus.FREE, repository.findById(slot.id()).orElseThrow().status());
    }

    @Test
    void translatesOptimisticLockFailureIntoSlotVersionConflict() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);
        repository.failNextSaveWith(new ObjectOptimisticLockingFailureException(TimeSlot.class, slot.id()));

        assertThrows(SlotVersionConflictException.class, () -> useCase.execute(slot.id(), ownerId, 0L));
    }
}
