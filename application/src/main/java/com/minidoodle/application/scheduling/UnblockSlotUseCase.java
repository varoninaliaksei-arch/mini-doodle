package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * POST /slots/{id}/unblock: BLOCKED -> FREE, owner-only. Mirrors
 * BlockSlotUseCase's shape; a BOOKED slot gets the same "cancel the meeting
 * first" treatment even though unblock() isn't the BOOKED-slot-release
 * transition itself, for the same reason: the owner-facing message should
 * name the actual blocker (an active meeting) rather than a generic state
 * error.
 */
public class UnblockSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public UnblockSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public TimeSlot execute(UUID slotId, UUID callerId, long expectedVersion) {
        TimeSlot current = SlotMutationGuard.requireMutable(timeSlotRepository, slotId, callerId, expectedVersion);

        current.unblock();

        try {
            return timeSlotRepository.save(current);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
