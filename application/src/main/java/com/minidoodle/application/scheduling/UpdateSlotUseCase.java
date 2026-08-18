package com.minidoodle.application.scheduling;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * PATCH /slots/{id}: both fields optional, merged with the slot's current
 * interval before validation (02-ARCHITECTURE.md §4). Owner-only, per the
 * uniform ownership-check policy (see README "Assumptions & trade-offs").
 */
public class UpdateSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public UpdateSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public TimeSlot execute(UUID slotId, UUID callerId, Optional<Instant> newStart, Optional<Instant> newEnd,
            long expectedVersion) {
        TimeSlot current = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));

        if (!current.calendarId().equals(callerId)) {
            throw new NotOwnerException("slot", slotId);
        }
        if (current.status() == SlotStatus.BOOKED) {
            throw new SlotBookedException(slotId);
        }
        if (current.version() != expectedVersion) {
            throw new SlotVersionConflictException(slotId, expectedVersion, current.version());
        }

        TimeInterval merged = new TimeInterval(
                newStart.orElse(current.interval().start()),
                newEnd.orElse(current.interval().end()));
        TimeSlot updated = new TimeSlot(current.id(), current.calendarId(), merged, current.status(),
                current.version());

        try {
            return timeSlotRepository.save(updated);
        } catch (DataIntegrityViolationException e) {
            throw new SlotConflictException(current.calendarId(), merged, e);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
