package com.minidoodle.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MeetingJpaRepository extends JpaRepository<MeetingJpaEntity, UUID> {

    Optional<MeetingJpaEntity> findByIdempotencyKey(String idempotencyKey);

    List<MeetingJpaEntity> findBySlotId(UUID slotId);
}
