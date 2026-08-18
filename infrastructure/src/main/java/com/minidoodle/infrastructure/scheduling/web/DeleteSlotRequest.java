package com.minidoodle.infrastructure.scheduling.web;

import io.swagger.v3.oas.annotations.media.Schema;

/** DELETE /slots/{id}: version travels in the body, not an ETag/If-Match header. */
public record DeleteSlotRequest(
        @Schema(description = "The slot's version as last read by the caller; a mismatch means it changed "
                + "concurrently and the request is rejected with 409.")
        long version) {
}
