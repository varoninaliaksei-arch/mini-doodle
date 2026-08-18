package com.minidoodle.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "participants")
public class ParticipantJpaEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "meeting_id", nullable = false)
    private MeetingJpaEntity meeting;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "user_id")
    private UUID userId;

    protected ParticipantJpaEntity() {
        // JPA
    }

    public ParticipantJpaEntity(String email, String displayName, UUID userId) {
        this.email = email;
        this.displayName = displayName;
        this.userId = userId;
    }

    void setMeeting(MeetingJpaEntity meeting) {
        this.meeting = meeting;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UUID getUserId() {
        return userId;
    }
}
