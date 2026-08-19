package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;

/** Owner-only, per the uniform ownership-check policy (see README "Assumptions & trade-offs"). */
public class DeleteSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public DeleteSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public void execute(UUID slotId, UUID callerId, long expectedVersion) {
        SlotMutationGuard.requireMutable(timeSlotRepository, slotId, callerId, expectedVersion);

        try {
            timeSlotRepository.deleteById(slotId);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
