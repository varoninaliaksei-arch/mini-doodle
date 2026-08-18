package com.minidoodle.infrastructure.scheduling.web;

import java.util.UUID;

public record ParticipantRequest(String email, String displayName, UUID userId) {
}
