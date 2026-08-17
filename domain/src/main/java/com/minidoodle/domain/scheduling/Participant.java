package com.minidoodle.domain.scheduling;

import java.util.UUID;

/**
 * Value object. Not a reference to a User entity — participants don't need
 * an account. No response/RSVP status: the system records
 * participants as metadata on the meeting but does not send invitations or
 * collect responses.
 */
public record Participant(String email, String displayName, UUID userId) {

    public Participant {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
