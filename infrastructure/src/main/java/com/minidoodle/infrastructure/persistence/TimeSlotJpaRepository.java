package com.minidoodle.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.minidoodle.domain.scheduling.SlotStatus;

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

    /**
     * Keyset page ordered by {@code (starts_at, id)}, matching the B-tree
     * index (02-ARCHITECTURE.md §10). {@code status} is nullable — a null
     * status skips the status filter, and Postgres can infer its type fine
     * since {@code t.status = :status} elsewhere in the predicate gives it
     * context. {@code afterStartsAt}/{@code afterId} need an explicit
     * {@code hasAfter} flag instead of the same "is null" trick: Postgres
     * can't determine the wire type of a timestamptz/uuid parameter that's
     * only ever compared via "is null" ("could not determine data type of
     * parameter" at the JDBC level) — hasAfter is a plain boolean, always
     * type-inferable, and afterStartsAt/afterId stay in a real comparison
     * context regardless of whether hasAfter is true.
     */
    @Query("""
            select t from TimeSlotJpaEntity t
            where t.calendarId = :calendarId
              and t.startsAt < :windowEnd
              and t.endsAt > :windowStart
              and (:status is null or t.status = :status)
              and (:hasAfter = false
                   or t.startsAt > :afterStartsAt
                   or (t.startsAt = :afterStartsAt and t.id > :afterId))
            order by t.startsAt asc, t.id asc
            """)
    List<TimeSlotJpaEntity> findPage(
            @Param("calendarId") UUID calendarId,
            @Param("windowStart") Instant windowStart,
            @Param("windowEnd") Instant windowEnd,
            @Param("status") SlotStatus status,
            @Param("hasAfter") boolean hasAfter,
            @Param("afterStartsAt") Instant afterStartsAt,
            @Param("afterId") UUID afterId,
            Pageable pageable);
}
