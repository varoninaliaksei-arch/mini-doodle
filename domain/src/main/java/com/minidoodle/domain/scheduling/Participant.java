package com.minidoodle.domain.scheduling;

import java.util.UUID;

/**
 * Value object, not a reference to a User entity — participants don't need
 * an account. No RSVP field: participants are metadata on the meeting
 * only; the system doesn't send invitations or collect responses.
 */
public record Participant(String email, String displayName, UUID userId) {

    public Participant {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
