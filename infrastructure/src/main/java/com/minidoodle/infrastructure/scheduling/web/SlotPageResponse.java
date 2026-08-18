package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;

/** {@code nextCursor} is {@code null} once there are no further pages. */
public record SlotPageResponse(List<SlotResponse> items, String nextCursor) {
}
