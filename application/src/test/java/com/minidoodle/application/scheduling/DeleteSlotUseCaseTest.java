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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteSlotUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final DeleteSlotUseCase useCase = new DeleteSlotUseCase(repository);

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));

    @Test
    void deletesAFreeSlot() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);

        useCase.execute(slot.id(), ownerId, 0L);

        assertFalse(repository.contains(slot.id()));
    }

    @Test
    void rejectsDeleteOfABookedSlotAndLeavesItInPlace() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.BOOKED, 0L);
        repository.seed(slot);

        assertThrows(SlotBookedException.class, () -> useCase.execute(slot.id(), ownerId, 0L));
        assertTrue(repository.contains(slot.id()));
    }

    @Test
    void rejectsStaleVersion() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 3L);
        repository.seed(slot);

        assertThrows(SlotVersionConflictException.class, () -> useCase.execute(slot.id(), ownerId, 2L));
        assertTrue(repository.contains(slot.id()));
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
        assertTrue(repository.contains(slot.id()));
    }

    @Test
    void translatesOptimisticLockFailureIntoSlotVersionConflict() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);
        repository.failNextDeleteWith(new ObjectOptimisticLockingFailureException(TimeSlot.class, slot.id()));

        assertThrows(SlotVersionConflictException.class, () -> useCase.execute(slot.id(), ownerId, 0L));
    }
}
