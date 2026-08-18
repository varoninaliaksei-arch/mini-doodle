package com.minidoodle.infrastructure.scheduling.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** POST /slots/{id}/block and .../unblock: version travels in the body, matching DELETE's convention. */
public record SlotVersionRequest(
        @Schema(description = "The slot's version as last read by the caller; a mismatch means it changed "
                + "concurrently and the request is rejected with 409.")
        long version) {
}
