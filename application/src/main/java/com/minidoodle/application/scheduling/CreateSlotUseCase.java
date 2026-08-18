package com.minidoodle.application.scheduling;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

public class CreateSlotUseCase {

    private final TimeSlotRepository timeSlotRepository;

    public CreateSlotUseCase(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    @Transactional
    public TimeSlot execute(UUID calendarId, TimeInterval interval) {
        TimeSlot slot = TimeSlot.free(UUID.randomUUID(), calendarId, interval);
        try {
            return timeSlotRepository.save(slot);
        } catch (DataIntegrityViolationException e) {
            throw new SlotConflictException(calendarId, interval, e);
        }
    }
}
