package com.minidoodle.infrastructure.scheduling.web;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.minidoodle.application.scheduling.TimeSlotCursor;

/**
 * {@code cursor = Base64(starts_at + id)}. Encode/decode lives in
 * infrastructure — application only ever sees the decoded
 * {@link TimeSlotCursor}.
 */
final class SlotCursorCodec {

    private static final String DELIMITER = "_";

    private SlotCursorCodec() {
    }

    static String encode(TimeSlotCursor cursor) {
        String raw = cursor.startsAt() + DELIMITER + cursor.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static TimeSlotCursor decode(String encoded) {
        String raw;
        try {
            raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed cursor: " + encoded, e);
        }

        int delimiterIndex = raw.lastIndexOf(DELIMITER);
        if (delimiterIndex < 0) {
            throw new IllegalArgumentException("Malformed cursor: " + encoded);
        }

        Instant startsAt = Instant.parse(raw.substring(0, delimiterIndex));
        UUID id = UUID.fromString(raw.substring(delimiterIndex + 1));
        return new TimeSlotCursor(startsAt, id);
    }
}
