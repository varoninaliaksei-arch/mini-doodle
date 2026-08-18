package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /slots/{id}/block: FREE -> BLOCKED (DOM-1), owner-only. A BOOKED slot
 * can't go directly to BLOCKED (the domain transition table forbids it), so
 * that case gets the same "cancel the meeting first" treatment as
 * DeleteSlotUseCase/UpdateSlotUseCase rather than a generic state-guard
 * error.
 */
public class BlockSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public BlockSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public TimeSlot execute(UUID slotId, UUID callerId, long expectedVersion) {
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

        current.block();

        try {
            return timeSlotRepository.save(current);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
