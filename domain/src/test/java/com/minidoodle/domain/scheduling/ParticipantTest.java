package com.minidoodle.domain.scheduling;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParticipantTest {

    @Test
    void equalWhenAllFieldsMatch() {
        UUID userId = UUID.randomUUID();
        Participant a = new Participant("a@example.com", "Alice", userId);
        Participant b = new Participant("a@example.com", "Alice", userId);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void notEqualWhenEmailDiffers() {
        Participant a = new Participant("a@example.com", "Alice", null);
        Participant b = new Participant("b@example.com", "Alice", null);

        assertNotEquals(a, b);
    }

    @Test
    void userIdCanBeNull() {
        Participant participant = new Participant("a@example.com", "Alice", null);

        assertNull(participant.userId());
    }

    @Test
    void rejectsBlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new Participant("   ", "Alice", null));
    }

    @Test
    void rejectsNullEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> new Participant(null, "Alice", null));
    }
}
