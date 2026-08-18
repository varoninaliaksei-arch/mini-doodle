package com.minidoodle.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.minidoodle.application.scheduling.MeetingRepository;
import com.minidoodle.domain.scheduling.Meeting;

@Repository
class MeetingRepositoryImpl implements MeetingRepository {

    private final MeetingJpaRepository jpaRepository;
    private final MeetingMapper mapper;

    MeetingRepositoryImpl(MeetingJpaRepository jpaRepository, MeetingMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<Meeting> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Meeting> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public Meeting save(Meeting meeting) {
        MeetingJpaEntity saved = jpaRepository.save(mapper.toEntity(meeting));
        return mapper.toDomain(saved);
    }
}
