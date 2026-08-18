-- Transactional outbox for domain events (02-ARCHITECTURE.md §8, §10). No
-- publisher exists yet — this table is written to only, in the same
-- transaction as the aggregate writes that produced the event (INFRA-3).

CREATE TABLE outbox_events (
    id           UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    type         VARCHAR(255) NOT NULL,
    payload      TEXT NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);
