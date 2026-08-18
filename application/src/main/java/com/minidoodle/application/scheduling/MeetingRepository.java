package com.minidoodle.application.scheduling;

import java.util.Optional;
import java.util.UUID;

import com.minidoodle.domain.scheduling.Meeting;

public interface MeetingRepository {

    Optional<Meeting> findById(UUID id);

    Optional<Meeting> findByIdempotencyKey(String idempotencyKey);

    Meeting save(Meeting meeting);
}
