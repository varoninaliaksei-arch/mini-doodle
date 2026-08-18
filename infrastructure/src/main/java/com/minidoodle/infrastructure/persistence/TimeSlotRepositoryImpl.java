package com.minidoodle.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.minidoodle.application.scheduling.TimeSlotCursor;
import com.minidoodle.application.scheduling.TimeSlotRepository;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

@Repository
class TimeSlotRepositoryImpl implements TimeSlotRepository {

    private final TimeSlotJpaRepository jpaRepository;
    private final TimeSlotMapper mapper;

    TimeSlotRepositoryImpl(TimeSlotJpaRepository jpaRepository, TimeSlotMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<TimeSlot> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    /**
     * saveAndFlush, not save: Hibernate otherwise defers the actual
     * UPDATE/INSERT to transaction-commit time, past the point where
     * callers try/catch DataIntegrityViolationException or
     * ObjectOptimisticLockingFailureException around this call - an
     * exception raised at the outer @Transactional commit boundary can't
     * be caught by application-layer code at all.
     */
    @Override
    public TimeSlot save(TimeSlot slot) {
        TimeSlotJpaEntity saved = jpaRepository.saveAndFlush(mapper.toEntity(slot));
        return mapper.toDomain(saved);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<TimeSlot> findByCalendarAndWindow(UUID calendarId, TimeInterval window) {
        return jpaRepository.findOverlapping(calendarId, window.start(), window.end())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TimeSlot> findPage(UUID calendarId, TimeInterval window, Optional<SlotStatus> status,
            Optional<TimeSlotCursor> after, int limit) {
        return jpaRepository.findPage(
                        calendarId,
                        window.start(),
                        window.end(),
                        status.orElse(null),
                        after.isPresent(),
                        after.map(TimeSlotCursor::startsAt).orElse(null),
                        after.map(TimeSlotCursor::id).orElse(null),
                        PageRequest.of(0, limit))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
