package com.minidoodle.application.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.minidoodle.application.scheduling.exception.BulkSlotLimitExceededException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateSlotsBulkUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final CreateSlotsBulkUseCase useCase = new CreateSlotsBulkUseCase(repository);

    @Test
    void createsBackToBackSlotsAndDiscardsTrailingRemainder() {
        UUID calendarId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T09:00:00Z");
        // 2h15m range at 1h slots -> 2 whole slots, 15m remainder discarded.
        TimeInterval range = new TimeInterval(start, start.plus(Duration.ofMinutes(135)));

        List<TimeSlot> created = useCase.execute(calendarId, range, Duration.ofHours(1));

        assertEquals(2, created.size());
        assertEquals(new TimeInterval(start, start.plus(Duration.ofHours(1))), created.get(0).interval());
        assertEquals(new TimeInterval(start.plus(Duration.ofHours(1)), start.plus(Duration.ofHours(2))),
                created.get(1).interval());
        created.forEach(slot -> assertEquals(SlotStatus.FREE, slot.status()));
    }

    @Test
    void rejectsRequestsAboveTheCapWithoutPersistingAnything() {
        UUID calendarId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        TimeInterval range = new TimeInterval(start, start.plus(Duration.ofMinutes(501)));

        assertThrows(BulkSlotLimitExceededException.class,
                () -> useCase.execute(calendarId, range, Duration.ofMinutes(1)));
        assertTrue(repository.findByCalendarAndWindow(calendarId, range).isEmpty());
    }

    @Test
    void translatesDataIntegrityViolationIntoSlotConflict() {
        UUID calendarId = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T09:00:00Z");
        TimeInterval range = new TimeInterval(start, start.plus(Duration.ofHours(2)));
        repository.failNextSaveWith(new DataIntegrityViolationException("exclusion constraint violated"));

        assertThrows(SlotConflictException.class, () -> useCase.execute(calendarId, range, Duration.ofHours(1)));
    }
}
