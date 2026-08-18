package com.minidoodle.application.scheduling;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.Meeting;

public interface MeetingRepository {

    Optional<Meeting> findById(UUID id);

    Optional<Meeting> findByIdempotencyKey(String idempotencyKey);

    List<Meeting> findBySlotId(UUID slotId);

    Meeting save(Meeting meeting);
}
