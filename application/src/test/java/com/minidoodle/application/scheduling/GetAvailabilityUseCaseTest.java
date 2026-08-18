package com.minidoodle.application.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.minidoodle.application.scheduling.exception.WindowTooLargeException;
import com.minidoodle.domain.scheduling.Coverage;
import com.minidoodle.domain.scheduling.CoverageInterval;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetAvailabilityUseCaseTest {

    private final InMemoryTimeSlotRepository repository = new InMemoryTimeSlotRepository();
    private final GetAvailabilityUseCase useCase = new GetAvailabilityUseCase(repository);

    private static final Instant DAY_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final TimeInterval WINDOW = new TimeInterval(DAY_START, DAY_START.plus(Duration.ofDays(1)));

    @Test
    void returnsMergedCoverageForTheWindow() {
        UUID calendarId = UUID.randomUUID();
        repository.seed(TimeSlot.free(UUID.randomUUID(), calendarId,
                new TimeInterval(DAY_START, DAY_START.plus(Duration.ofHours(1)))));

        List<CoverageInterval> coverage = useCase.execute(calendarId, WINDOW);

        assertEquals(2, coverage.size());
        assertEquals(Coverage.FREE, coverage.get(0).coverage());
        assertEquals(Coverage.UNAVAILABLE, coverage.get(1).coverage());
    }

    @Test
    void rejectsWindowsAboveTheCap() {
        UUID calendarId = UUID.randomUUID();
        TimeInterval tooWide = new TimeInterval(DAY_START, DAY_START.plus(Duration.ofDays(91)));

        assertThrows(WindowTooLargeException.class, () -> useCase.execute(calendarId, tooWide));
    }
}
