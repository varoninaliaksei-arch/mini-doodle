package com.minidoodle.application.scheduling;

import java.util.UUID;

import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeSlot;

/**
 * Shared preamble for slot mutations (block/unblock/delete/update): load,
 * check ownership, refuse a BOOKED slot ("cancel the meeting first"), check
 * the optimistic-lock version — in that order, matching what each use case's
 * tests assert individually.
 */
final class SlotMutationGuard {

    private SlotMutationGuard() {
    }

    static TimeSlot requireMutable(TimeSlotRepository timeSlotRepository, UUID slotId, UUID callerId,
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

        return current;
    }
}
