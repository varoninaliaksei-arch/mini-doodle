package com.minidoodle.domain.scheduling;

import java.util.Objects;
import java.util.UUID;

public record MeetingDetails(String title, String description, UUID organizerId) {

    public MeetingDetails {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Objects.requireNonNull(organizerId, "organizerId must not be null");
    }
}
