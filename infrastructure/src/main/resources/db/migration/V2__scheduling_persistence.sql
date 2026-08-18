-- Persistence for the TimeSlot and Meeting aggregates (02-ARCHITECTURE.md §5, §6, §10).
-- calendar_id / organizer_id are plain UUID columns, no FK: the Calendar and
-- User aggregates are not persisted yet in this slice.

CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE time_slots (
    id          UUID PRIMARY KEY,
    calendar_id UUID NOT NULL,
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    status      VARCHAR(255) NOT NULL,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT no_overlapping_slots EXCLUDE USING gist (
        calendar_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&
    )
);

CREATE INDEX idx_time_slots_calendar_starts_at ON time_slots (calendar_id, starts_at);

CREATE TABLE meetings (
    id              UUID PRIMARY KEY,
    slot_id         UUID NOT NULL REFERENCES time_slots (id),
    title           VARCHAR(255) NOT NULL,
    description     VARCHAR(255),
    organizer_id    UUID NOT NULL,
    status          VARCHAR(255) NOT NULL,
    idempotency_key TEXT,
    version         BIGINT NOT NULL DEFAULT 0
);

-- SCOPE-1: Idempotency-Key is an optional request header, so the column is
-- nullable; uniqueness only applies when a key was actually supplied. A
-- partial unique index is the only way to express that in Postgres — it
-- can't be written as an inline table CONSTRAINT.
CREATE UNIQUE INDEX idx_meetings_idempotency_key ON meetings (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE participants (
    id           UUID PRIMARY KEY,
    meeting_id   UUID NOT NULL REFERENCES meetings (id) ON DELETE CASCADE,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    user_id      UUID
);

CREATE INDEX idx_participants_meeting_id ON participants (meeting_id);
