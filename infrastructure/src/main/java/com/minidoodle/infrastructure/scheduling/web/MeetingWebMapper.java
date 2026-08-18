package com.minidoodle.infrastructure.scheduling.web;

import org.springframework.stereotype.Component;

import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.Participant;

@Component
class MeetingWebMapper {

    MeetingResponse toResponse(Meeting meeting) {
        return new MeetingResponse(
                meeting.id(),
                meeting.slotId(),
                meeting.details().title(),
                meeting.details().description(),
                meeting.details().organizerId().toString(),
                meeting.status(),
                meeting.participants().stream().map(this::toResponse).toList());
    }

    private ParticipantResponse toResponse(Participant participant) {
        return new ParticipantResponse(participant.email(), participant.displayName());
    }
}
