package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreateSlotUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final CreateSlotUseCase useCase = new CreateSlotUseCase(repository);

    private static final TimeInterval INTERVAL =
            new TimeInterval(Instant.parse("2026-01-01T10:00:00Z"), Instant.parse("2026-01-01T11:00:00Z"));

    @Test
    void createsAFreeSlotAndPersistsIt() {
        UUID calendarId = UUID.randomUUID();

        TimeSlot created = useCase.execute(calendarId, INTERVAL);

        assertEquals(calendarId, created.calendarId());
        assertEquals(INTERVAL, created.interval());
        assertEquals(SlotStatus.FREE, created.status());
        assertEquals(created, repository.findById(created.id()).orElseThrow());
    }

    @Test
    void translatesDataIntegrityViolationIntoSlotConflict() {
        repository.failNextSaveWith(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThrows(SlotConflictException.class, () -> useCase.execute(UUID.randomUUID(), INTERVAL));
    }
}
