package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /slots/{id}/block: FREE -> BLOCKED, owner-only. A BOOKED slot
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
        TimeSlot current = SlotMutationGuard.requireMutable(timeSlotRepository, slotId, callerId, expectedVersion);

        current.block();

        try {
            return timeSlotRepository.save(current);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
