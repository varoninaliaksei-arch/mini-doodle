package com.minidoodle.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.minidoodle.application.scheduling.TimeSlotRepository;
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

    @Override
    public TimeSlot save(TimeSlot slot) {
        TimeSlotJpaEntity saved = jpaRepository.save(mapper.toEntity(slot));
        return mapper.toDomain(saved);
    }

    @Override
    public List<TimeSlot> findByCalendarAndWindow(UUID calendarId, TimeInterval window) {
        return jpaRepository.findOverlapping(calendarId, window.start(), window.end())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
