package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record SlotPageResponse(
        List<SlotResponse> items,
        @Schema(description = "Opaque keyset-pagination token (Base64 of the last item's startsAt + id); "
                + "pass it back as the ?cursor= query param to fetch the next page. Null once there are no "
                + "further pages.", nullable = true)
        String nextCursor) {
}
