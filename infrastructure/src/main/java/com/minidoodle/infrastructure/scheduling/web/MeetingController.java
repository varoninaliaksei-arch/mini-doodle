package com.minidoodle.infrastructure.scheduling.web;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minidoodle.application.scheduling.CancelMeetingUseCase;
import com.minidoodle.domain.scheduling.Meeting;

@RestController
@RequestMapping("/meetings")
class MeetingController {

    private final CancelMeetingUseCase cancelMeetingUseCase;
    private final MeetingWebMapper mapper;

    MeetingController(CancelMeetingUseCase cancelMeetingUseCase, MeetingWebMapper mapper) {
        this.cancelMeetingUseCase = cancelMeetingUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/{id}/cancel")
    MeetingResponse cancel(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID callerId) {
        Meeting cancelled = cancelMeetingUseCase.execute(id, callerId);
        return mapper.toResponse(cancelled);
    }
}
