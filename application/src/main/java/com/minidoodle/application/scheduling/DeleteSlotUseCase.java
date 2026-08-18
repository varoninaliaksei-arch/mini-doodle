package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeSlot;

public class DeleteSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public DeleteSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public void execute(UUID slotId, long expectedVersion) {
        TimeSlot current = timeSlotRepository.findById(slotId)
                .orElseThrow(() -> new SlotNotFoundException(slotId));

        if (current.status() == SlotStatus.BOOKED) {
            throw new SlotBookedException(slotId);
        }
        if (current.version() != expectedVersion) {
            throw new SlotVersionConflictException(slotId, expectedVersion, current.version());
        }

        try {
            timeSlotRepository.deleteById(slotId);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SlotVersionConflictException(slotId, e);
        }
    }
}
