package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateSlotUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final UpdateSlotUseCase useCase = new UpdateSlotUseCase(repository);

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));

    @Test
    void mergesPartialUpdateWithCurrentIntervalOnAFreeSlot() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);
        Instant newStart = Instant.parse("2026-01-01T09:30:00Z");

        TimeSlot updated = useCase.execute(slot.id(), ownerId, Optional.of(newStart), Optional.empty(), 0L);

        assertEquals(new TimeInterval(newStart, INTERVAL.end()), updated.interval());
    }

    @Test
    void rejectsUpdateOfABookedSlot() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.BOOKED, 0L);
        repository.seed(slot);

        assertThrows(SlotBookedException.class,
                () -> useCase.execute(slot.id(), ownerId, Optional.empty(), Optional.empty(), 0L));
    }

    @Test
    void rejectsStaleVersion() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 3L);
        repository.seed(slot);

        assertThrows(SlotVersionConflictException.class,
                () -> useCase.execute(slot.id(), ownerId, Optional.empty(), Optional.empty(), 2L));
    }

    @Test
    void slotNotFoundThrows() {
        assertThrows(SlotNotFoundException.class,
                () -> useCase.execute(UUID.randomUUID(), UUID.randomUUID(), Optional.empty(), Optional.empty(), 0L));
    }

    @Test
    void rejectsCallerWhoIsNotTheOwner() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);

        assertThrows(NotOwnerException.class,
                () -> useCase.execute(slot.id(), UUID.randomUUID(), Optional.empty(), Optional.empty(), 0L));
    }

    @Test
    void translatesDataIntegrityViolationIntoSlotConflict() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);
        repository.failNextSaveWith(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThrows(SlotConflictException.class,
                () -> useCase.execute(slot.id(), ownerId, Optional.empty(), Optional.empty(), 0L));
    }

    @Test
    void translatesOptimisticLockFailureIntoSlotVersionConflict() {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot = new TimeSlot(UUID.randomUUID(), ownerId, INTERVAL, SlotStatus.FREE, 0L);
        repository.seed(slot);
        repository.failNextSaveWith(new ObjectOptimisticLockingFailureException(TimeSlot.class, slot.id()));

        assertThrows(SlotVersionConflictException.class,
                () -> useCase.execute(slot.id(), ownerId, Optional.empty(), Optional.empty(), 0L));
    }
}
