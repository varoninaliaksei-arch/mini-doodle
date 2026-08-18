package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import com.minidoodle.domain.scheduling.MeetingStatus;

public record MeetingResponse(
        UUID id,
        UUID slotId,
        String title,
        String description,
        @Schema(description = "The X-User-Id of the caller who created the booking.") String organizerId,
        MeetingStatus status,
        List<ParticipantResponse> participants) {
}
