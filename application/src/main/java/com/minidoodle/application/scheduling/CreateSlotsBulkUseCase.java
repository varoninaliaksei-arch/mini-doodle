package com.minidoodle.application.scheduling;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.BulkSlotLimitExceededException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * Range + slotDuration -> N back-to-back slots, all-or-nothing in one
 * transaction. A trailing remainder that doesn't fill a whole slot is
 * discarded, never rounded up.
 */
public class CreateSlotsBulkUseCase {

    public static final int MAX_SLOTS_PER_CALL = 500;

    private final TimeSlotRepository timeSlotRepository;

    public CreateSlotsBulkUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public List<TimeSlot> execute(UUID calendarId, TimeInterval range, Duration slotDuration) {
        if (slotDuration == null) {
            throw new IllegalArgumentException("slotDuration must not be null");
        }
        if (slotDuration.isZero() || slotDuration.isNegative()) {
            throw new IllegalArgumentException("slotDuration must be positive");
        }

        long slotCount = Duration.between(range.start(), range.end()).dividedBy(slotDuration);
        if (slotCount > MAX_SLOTS_PER_CALL) {
            throw new BulkSlotLimitExceededException(slotCount, MAX_SLOTS_PER_CALL);
        }

        List<TimeSlot> slots = new ArrayList<>();
        Instant cursor = range.start();
        for (long i = 0; i < slotCount; i++) {
            Instant slotEnd = cursor.plus(slotDuration);
            slots.add(TimeSlot.free(UUID.randomUUID(), calendarId, new TimeInterval(cursor, slotEnd)));
            cursor = slotEnd;
        }

        try {
            List<TimeSlot> saved = new ArrayList<>();
            for (TimeSlot slot : slots) {
                saved.add(timeSlotRepository.save(slot));
            }
            return saved;
        } catch (DataIntegrityViolationException e) {
            throw new SlotConflictException(calendarId, range, e);
        }
    }
}
