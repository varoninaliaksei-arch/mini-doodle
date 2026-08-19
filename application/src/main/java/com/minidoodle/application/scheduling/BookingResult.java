package com.minidoodle.application.scheduling;

import java.util.Objects;

import com.minidoodle.domain.scheduling.Meeting;

/**
 * Outcome of {@link CreateBookingUseCase#execute}: {@code created} is
 * {@code false} when the Idempotency-Key short-circuit returned an
 * existing meeting rather than booking anything new — the REST layer
 * uses this to answer {@code 200} on a replay vs. {@code 201} on an
 * actual creation, while the response body stays identical either way.
 */
public record BookingResult(Meeting meeting, boolean created) {

    public BookingResult {
        Objects.requireNonNull(meeting, "meeting must not be null");
    }
}
