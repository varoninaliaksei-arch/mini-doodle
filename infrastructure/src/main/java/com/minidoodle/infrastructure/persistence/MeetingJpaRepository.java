package com.minidoodle.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MeetingJpaRepository extends JpaRepository<MeetingJpaEntity, UUID> {
}
