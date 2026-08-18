package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import com.minidoodle.domain.scheduling.MeetingStatus;

public record MeetingResponse(UUID id, UUID slotId, String title, String description, String organizerId,
        MeetingStatus status, List<ParticipantResponse> participants) {
}
