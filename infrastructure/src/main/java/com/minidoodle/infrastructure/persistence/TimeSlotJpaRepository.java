package com.minidoodle.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface TimeSlotJpaRepository extends JpaRepository<TimeSlotJpaEntity, UUID> {

    @Query("""
            select t from TimeSlotJpaEntity t
            where t.calendarId = :calendarId
              and t.startsAt < :windowEnd
              and t.endsAt > :windowStart
            """)
    List<TimeSlotJpaEntity> findOverlapping(
            @Param("calendarId") UUID calendarId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd);
}
