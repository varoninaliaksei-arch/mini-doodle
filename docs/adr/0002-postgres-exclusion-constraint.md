# 0002. Postgres exclusion constraint as the concurrency mechanism

## Status
Accepted

## Context
Non-overlap of slots within a calendar is an invariant across *multiple
rows*, with no single row that owns it. Two concurrently inserted slots can
each check "no overlaps", each see a valid state, and both commit — classic
write skew, which `READ COMMITTED` does not catch. The standard fixes are
`SERIALIZABLE` isolation (expensive, rollback-prone under load) or
materializing the invariant into a single row — impossible here, since no
such row exists for a range-based invariant. Double booking is explicitly
unacceptable (`NFR-2`); a `409` refusal is an acceptable outcome.

## Decision
```sql
CREATE EXTENSION btree_gist;

ALTER TABLE time_slots ADD CONSTRAINT no_overlapping_slots
  EXCLUDE USING gist (
    calendar_id WITH =,
    tstzrange(starts_at, ends_at, '[)') WITH &&
  );
```
The database evaluates the predicate as part of the write — a
generalization of a plain `UNIQUE` constraint, from equality to range
overlap via GiST. `@Version` (optimistic locking) sits on top purely to
turn the resulting `SQLException` into a clean `409`; the constraint
remains the actual source of truth. Alternatives rejected: `SELECT FOR
UPDATE` (serializes all writes to a calendar), a Redis lock (a *weaker*
guarantee than the database — a regression, not an optimization), an
in-application overlap check (a race by construction).

## Consequences
Holds regardless of the number of application instances and cannot be
bypassed by a service-layer bug — verified directly by the mandatory
concurrency test (`BookingRaceIT`: 50 parallel bookings on one slot, exactly
one `201`). The GiST index the constraint requires is reused by the
free/busy read path at no extra cost. Trade-off: the constraint's shape is
tied to the current domain shape (one slot, one calendar, whole-slot
booking); slot capacity would need a counter invariant instead, which
cannot be expressed this way (see `DOM-5`).
