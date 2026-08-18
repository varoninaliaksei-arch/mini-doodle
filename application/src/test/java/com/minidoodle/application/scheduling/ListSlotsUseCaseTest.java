package com.minidoodle.application.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minidoodle.application.scheduling.exception.WindowTooLargeException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListSlotsUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final ListSlotsUseCase useCase = new ListSlotsUseCase(repository);

    private static final Instant DAY_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeInterval WINDOW = new TimeInterval(DAY_START, DAY_START.plus(Duration.ofDays(1)));

    @Test
    void returnsAPageAndFlagsWhenMoreRemain() {
        UUID calendarId = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            Instant start = DAY_START.plus(Duration.ofHours(i));
            repository.seed(TimeSlot.free(UUID.randomUUID(), calendarId,
                    new TimeInterval(start, start.plus(Duration.ofMinutes(30)))));
        }

        SlotPage page = useCase.execute(calendarId, WINDOW, Optional.empty(), Optional.empty(), 2);

        assertEquals(2, page.items().size());
        assertTrue(page.hasMore());
    }

    @Test
    void lastPageReportsNoMore() {
        UUID calendarId = UUID.randomUUID();
        repository.seed(TimeSlot.free(UUID.randomUUID(), calendarId,
                new TimeInterval(DAY_START, DAY_START.plus(Duration.ofMinutes(30)))));

        SlotPage page = useCase.execute(calendarId, WINDOW, Optional.empty(), Optional.empty(), 10);

        assertEquals(1, page.items().size());
        assertFalse(page.hasMore());
    }

    @Test
    void rejectsWindowsAboveTheCap() {
        UUID calendarId = UUID.randomUUID();
        TimeInterval tooWide = new TimeInterval(DAY_START, DAY_START.plus(Duration.ofDays(91)));

        assertThrows(WindowTooLargeException.class,
                () -> useCase.execute(calendarId, tooWide, Optional.empty(), Optional.empty(), 10));
    }

    @Test
    void filtersByStatusWhenProvided() {
        UUID calendarId = UUID.randomUUID();
        TimeSlot free = TimeSlot.free(UUID.randomUUID(), calendarId,
                new TimeInterval(DAY_START, DAY_START.plus(Duration.ofMinutes(30))));
        TimeSlot blocked = new TimeSlot(UUID.randomUUID(), calendarId,
                new TimeInterval(DAY_START.plus(Duration.ofHours(1)), DAY_START.plus(Duration.ofHours(2))),
                SlotStatus.BLOCKED, 0L);
        repository.seed(free);
        repository.seed(blocked);

        SlotPage page = useCase.execute(calendarId, WINDOW, Optional.of(SlotStatus.FREE), Optional.empty(), 10);

        assertEquals(1, page.items().size());
        assertEquals(free.id(), page.items().get(0).id());
    }
}
