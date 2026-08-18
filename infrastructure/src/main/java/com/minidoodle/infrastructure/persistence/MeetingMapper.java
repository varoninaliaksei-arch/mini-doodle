package com.minidoodle.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.Participant;

@Component
public class MeetingMapper {

    public Meeting toDomain(MeetingJpaEntity entity) {
        List<Participant> participants = entity.getParticipants().stream()
                .map(p -> new Participant(p.getEmail(), p.getDisplayName(), p.getUserId()))
                .toList();

        return new Meeting(
                entity.getId(),
                entity.getSlotId(),
                new MeetingDetails(entity.getTitle(), entity.getDescription(), entity.getOrganizerId()),
                participants,
                entity.getStatus(),
                entity.getIdempotencyKey());
    }

    public MeetingJpaEntity toEntity(Meeting meeting) {
        List<ParticipantJpaEntity> participants = new ArrayList<>();
        for (Participant participant : meeting.participants()) {
            participants.add(new ParticipantJpaEntity(participant.email(), participant.displayName(),
                    participant.userId()));
        }

        MeetingDetails details = meeting.details();
        return new MeetingJpaEntity(
                meeting.id(),
                meeting.slotId(),
                details.title(),
                details.description(),
                details.organizerId(),
                meeting.status(),
                meeting.idempotencyKey(),
                participants);
    }
}
