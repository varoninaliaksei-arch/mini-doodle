# 0003. Transactional outbox instead of a message broker

## Status
Accepted

## Context
Booking a slot must atomically flip the slot to `BOOKED`, create the
`Meeting`, and record that `MeetingBooked` happened, so a future consumer
(a notification service, a sync service) can react. No such consumer exists
yet — publishing straight to a broker would add infrastructure without a
purpose, and a dual write (commit to Postgres, then publish to a broker)
reintroduces exactly the atomicity problem the design is trying to avoid.

## Decision
An `outbox_events` table, written in the same transaction as the slot and
meeting writes:
```
BEGIN
  UPDATE time_slots SET status = 'BOOKED' ...
  INSERT INTO meetings ...
  INSERT INTO outbox_events (type = 'MeetingBooked', payload = ..., ...)
COMMIT
```
A `@Scheduled` `OutboxPublisher` polls unpublished rows and publishes them —
today that means logging; swapping in `KafkaTemplate` later touches only
the publisher, not the write path. Events are `MeetingBooked` and
`MeetingCancelled`; there is deliberately no `SlotReleased` — the only path
`BOOKED → FREE` is meeting cancellation, so slot release is always a
consequence of `MeetingCancelled`, never a standalone fact.

## Consequences
Delivery is at-least-once; any future consumer must be idempotent. The
event schema omits an `occurredAt` field — `outbox_events.created_at`,
written in the same transaction, is the single source of timestamp truth
and would only diverge from a duplicated domain-event timestamp if outbox
writes were ever batched or delayed (not the case today). The connection
point for a real broker is physically visible as one table and one
publisher class, at a cost of roughly half an hour versus standing up
Kafka now — see `0004-no-redis-no-kafka.md`.
