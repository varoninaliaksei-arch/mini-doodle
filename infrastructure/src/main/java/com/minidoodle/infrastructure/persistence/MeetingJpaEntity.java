package com.minidoodle.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minidoodle.domain.scheduling.MeetingStatus;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "meetings")
public class MeetingJpaEntity {

    @Id
    private UUID id;

    @Column(name = "slot_id", nullable = false)
    private UUID slotId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MeetingStatus status;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @OneToMany(mappedBy = "meeting", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<ParticipantJpaEntity> participants = new ArrayList<>();

    protected MeetingJpaEntity() {
        // JPA
    }

    public MeetingJpaEntity(UUID id, UUID slotId, String title, String description, UUID organizerId,
            MeetingStatus status, String idempotencyKey, List<ParticipantJpaEntity> participants) {
        this.id = id;
        this.slotId = slotId;
        this.title = title;
        this.description = description;
        this.organizerId = organizerId;
        this.status = status;
        this.idempotencyKey = idempotencyKey;
        for (ParticipantJpaEntity participant : participants) {
            participant.setMeeting(this);
            this.participants.add(participant);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getSlotId() {
        return slotId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public UUID getOrganizerId() {
        return organizerId;
    }

    public MeetingStatus getStatus() {
        return status;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public long getVersion() {
        return version;
    }

    public List<ParticipantJpaEntity> getParticipants() {
        return participants;
    }
}
