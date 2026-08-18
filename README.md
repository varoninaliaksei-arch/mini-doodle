# mini-doodle

## 1. What this is

A booking-page / 1:1 scheduling engine: an owner defines available time
slots on a personal calendar, a slot converts into a meeting with a title,
description and participants when booked, and the API exposes an
aggregated free/busy view over a time window. This is not a Doodle Group
Poll — there is no voting or consensus step, and no participant response
is collected.

## 2. Quick start

Requires Docker and Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

- App: `http://localhost:8080`
- Health check (readiness): `http://localhost:8080/actuator/health/readiness`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Flyway migrations run automatically on application startup — there is no
separate migration step. `app` waits on `postgres` via a healthcheck
(`condition: service_healthy`), so the first `docker compose up` doesn't
race the database.

## 3. How to consume the API

All examples assume the app is running per §2. `X-User-Id` is a stub
identity header (see §6) — any UUID acts as an authenticated user, no
account/password exists to obtain one. Times are ISO-8601 UTC.

Full request/response contract (all fields, error shapes, pagination) is
in Swagger UI — this section is a walkthrough, not the reference.

**1. Create a slot**

```bash
curl -s -X POST http://localhost:8080/slots \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 11111111-1111-1111-1111-111111111111' \
  -d '{"startsAt": "2026-08-20T09:00:00Z", "endsAt": "2026-08-20T09:30:00Z"}'
```
Returns `201` with the created `SlotResponse` (id, status `FREE`, `version`).

**2. Book it**

```bash
curl -s -X POST http://localhost:8080/bookings \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: 22222222-2222-2222-2222-222222222222' \
  -H 'Idempotency-Key: 6f2a1e6e-...' \
  -d '{
    "slotId": "<slot id from step 1>",
    "title": "Intro call",
    "description": "15 min sync",
    "participants": [{"email": "a@example.com", "displayName": "Alice"}]
  }'
```
Returns `201` with a `MeetingResponse`, and flips the slot to `BOOKED` in
the same transaction. `Idempotency-Key` is optional; a retried request with
the same key returns the original meeting instead of creating a duplicate.
Booking an already-taken slot returns `409`.

**3. Query availability**

```bash
curl -s "http://localhost:8080/availability?ownerId=11111111-1111-1111-1111-111111111111&from=2026-08-20T00:00:00Z&to=2026-08-21T00:00:00Z"
```
Returns merged `BUSY` / `FREE` / `UNAVAILABLE` intervals — a
continuous-coverage answer, not a slot list, and doesn't reveal whether a
`BUSY` interval is a booking or a manual block. For a raw, filterable slot
list (e.g. to find bookable slot ids), use `GET /slots?ownerId=&from=&to=&status=FREE`
instead, which is keyset-paginated via `cursor`.

**4. Cancel the meeting**

```bash
curl -s -X POST http://localhost:8080/meetings/<meeting id>/cancel
```
Returns `200` with the cancelled `MeetingResponse` and releases the slot
back to `FREE` — the only path a booked slot returns to `FREE`. Deleting or
modifying a booked slot directly returns `409` ("cancel the meeting
first").

## 4. Architecture at a glance

Three Gradle modules, one deployable service, dependencies strictly
one-directional:

```
domain → application → infrastructure
```

- **`domain`** — zero external dependencies: no Spring, no JPA. Aggregates
  `TimeSlot` and `Meeting`, value objects (`TimeInterval`, `Participant`,
  `SlotStatus`), and the pure `AvailabilityAggregator.aggregate(...)`
  function behind the free/busy view.
- **`application`** — use cases and `@Transactional` boundaries; depends
  only on `domain`. Repository ports (interfaces) live here; no JPA or HTTP
  types cross into this module's signatures.
- **`infrastructure`** — the only module with Spring Boot, Hibernate/JPA,
  REST controllers, Flyway, and `@SpringBootApplication`. JPA entities and
  `toDomain()`/`toEntity()` mappers are the only code that sees both the
  domain model and the persistence model.

The boundary is structural, not a lint rule: `domain/build.gradle.kts`
declares no dependencies, so pulling in Spring or Hibernate there is a
compile failure, not a test that can be weakened or skipped. See
[`docs/adr/0001-modular-monolith.md`](docs/adr/0001-modular-monolith.md).

Three aggregate roots, kept independent rather than nested, so a slot
operation never requires loading a calendar's full slot collection:

- **`TimeSlot`** — `FREE` / `BLOCKED` / `BOOKED`, half-open UTC interval.
- **`Meeting`** — created via `schedule(...)`, records `MeetingBooked`;
  `cancel()` records `MeetingCancelled`.
- **`Calendar`** — thin identity (`id`, `ownerUserId`, `timezone`), no
  behavior. Internal only: it does not appear in any REST path, request/
  response body, or the OpenAPI schema. The client works with slots and
  meetings; the calendar is derived from the owner.

## 5. Key design decisions

**The Postgres exclusion constraint is the concurrency mechanism, not
application code.** Non-overlap of slots within a calendar is an invariant
across multiple rows with no natural row owner — two concurrent inserts can
each see a valid state and both commit, which is write skew, and
`READ COMMITTED` does not catch it. Rather than `SERIALIZABLE` isolation
(expensive, rollback-prone under load) or an application-level check (a
race by construction), the invariant is pushed into the database:
```sql
EXCLUDE USING gist (calendar_id WITH =, tstzrange(starts_at, ends_at, '[)') WITH &&)
```
This holds regardless of replica count and can't be bypassed by a
service-layer bug; the required GiST index is reused by the free/busy read
path for free. A violation surfaces as Spring's
`DataIntegrityViolationException`, caught at the use-case boundary and
mapped to a clean `409` — the constraint remains the actual source of
truth, this is only exception translation. `@Version` is a separate
mechanism on top of it, catching stale concurrent updates to the same row
(`ObjectOptimisticLockingFailureException`), mapped to `409` the same way.
Verified directly by `BookingRaceIT` (§10). See
[`docs/adr/0002-postgres-exclusion-constraint.md`](docs/adr/0002-postgres-exclusion-constraint.md).

**Consistency is strict by construction, not as a CAP trade-off.** With a
single Postgres node there is no network partition inside the storage
layer, so CAP does not literally apply — the booking invariant is enforced
transactionally against one source of truth. CAP becomes real only at the
read-replica step of the scaling path (§8): free/busy reads would become
eventually consistent against a replica while bookings stay strictly
consistent against the primary. That split — by consistency requirement,
not by convenience — is deliberate.

**Redis and Kafka are excluded deliberately, with named thresholds, not
because they were overlooked.** At the stated scale (~10⁴ slot rows,
bookings rare, read:write ≳ 20:1), adding a broker or cache to the base
compose profile would signal an inability to estimate load rather than an
ability to plan for it.

| Component | Verdict | Reconsider when |
|---|---|---|
| Kafka | Excluded | A real second consumer of domain events appears |
| Redis (cache) | Excluded | Postgres read latency stops meeting SLA at higher row counts |
| Redis (locks) | Excluded, structurally | Never — a strictly weaker guarantee than the exclusion constraint |
| API gateway | Excluded | A second service exists to route to |
| Elasticsearch | Excluded | Full-text search enters scope |

See [`docs/adr/0004-no-redis-no-kafka.md`](docs/adr/0004-no-redis-no-kafka.md).

**Booking events go through a transactional outbox, not a broker, because
no consumer exists yet.** `outbox_events` is written in the same
transaction as the slot and meeting writes; a `@Scheduled` publisher drains
it (logs today, swaps to `KafkaTemplate` later without touching the write
path). This solves the dual-write problem without standing up
infrastructure for a consumer that doesn't exist. See
[`docs/adr/0003-transactional-outbox.md`](docs/adr/0003-transactional-outbox.md).

**Three distinct concurrency mechanisms address three distinct
situations** — conflating them would either weaken a guarantee or add
unneeded machinery:

| Situation | Mechanism |
|---|---|
| Overlapping slots in a calendar | exclusion constraint |
| Re-booking an already-taken slot | slot status check → `409` |
| Retry / double-click of one request | `Idempotency-Key` |

## 6. Assumptions & trade-offs

- **`X-User-Id` header stub, not real auth.** No JWT/OAuth — out of scope
  for this assignment; the header stands in for an authenticated identity.
- **No participant notifications or RSVP.** Participants are recorded as
  metadata (email, display name) on the meeting; no invitation-delivery
  channel exists, so an RSVP/response field could never be populated —
  dead code rather than a placeholder.
- **No slot capacity.** A slot is booked entirely by one meeting. Capacity
  would replace the overlap invariant with a counter invariant, which
  can't be expressed as a declarative Postgres constraint and would need
  row-level locking — trading away the strongest decision in the solution
  for a feature not in the brief.
- **No TTL holds/reservations on a slot.** Booking is a single atomic
  `POST /bookings`; there is no multi-step pick → pay → confirm flow, so
  there is nothing to hold between steps.

## 7. What I would do with more time

Distinct from the scaling path (§8), which is about growth — this is about
what was deliberately cut within the assignment's current scope:

- Broader test coverage: use-case tests currently run against in-memory
  fakes rather than a full Testcontainers-backed integration suite for
  every endpoint (only the booking race is Testcontainers-backed today).
- `POST /meetings/{id}/reschedule` as a distinct operation from cancel +
  recreate, with its own participant-notification semantics.
- Locking participant calendars in addition to the owner's — currently
  booking only locks the slot owner's calendar; participants receive no
  calendar hold at all, since they aren't modeled as users (§6).
- `Idempotency-Key` support on other write endpoints (slot create/update,
  cancel) — currently only `POST /bookings` has it.
- Audit history on slot/meeting state transitions, rather than only the
  current state.

## 8. Scaling path

Documented, not implemented — thresholds, not a timeline:

| # | Step | Trigger |
|---|---|---|
| 0 | Single Postgres | Up to ~10⁸ rows. **Current state.** |
| 1 | HikariCP tuning + PgBouncer | Connection count approaches `max_connections` |
| 2 | Read replicas for free/busy | read:write exceeds ~20:1 |
| 3 | Time-based partitioning (`RANGE (starts_at)`, monthly, `pg_partman`) | Hot window (±3 months) stops fitting `shared_buffers` |
| 4 | Sharding by `calendar_id` | Write throughput saturates the primary |

Partitioning caveat: the exclusion constraint can't be globally unique
across partitions — the partition key has to become part of the
constraint, so it ends up enforced per-partition rather than globally.
Sharding is deferred deliberately: bookings are rare writes, and a primary
Postgres comfortably handles thousands of write TPS at this domain's
shape — sharding solves a problem this service doesn't have yet.

## 9. Observability

```bash
docker compose --profile observability up -d
```

Adds Prometheus (`localhost:9090`) and Grafana (`localhost:3000`, login
`admin` / `admin` — dev-only credentials, not meant to be exposed) on top of
the base `app` + `postgres` profile (INFRA-1 stays intact: plain
`docker compose up`, no `--profile`, is still exactly two containers).
Prometheus scrapes `app:8080/actuator/prometheus` every 15s
(`observability/prometheus/prometheus.yml`); Grafana's datasource and
dashboard are wired entirely through provisioning files under
`observability/grafana/` — nothing is clicked together in the UI, so the
dashboard is there on first boot, not after a manual import step.

Open `http://localhost:3000/d/mini-doodle-business-metrics` for the
**mini-doodle - business metrics** dashboard, one panel per SCOPE-3
hand-written metric (Micrometer/Spring Boot give the technical metrics —
JVM, HTTP, datasource pool — for free; nobody reviewing this is going to
browse a bare `/actuator/prometheus`, so these four are the ones worth a
graph):

- **Booking attempts (rate by result)** — `booking_attempts_total{result}`,
  split into `success` / `conflict` / `not_found`. A `conflict`/`not_found`
  spike that isn't matched by a `success` spike is the signal that
  something's wrong upstream of the database (stale slot ids, clients
  hammering the same slot) rather than noise the `409`/`404` already
  absorbed.
- **Booking duration (p50 / p95)** — `booking_duration_seconds_bucket` via
  `histogram_quantile()`. p95 pulling away from p50 is the early warning
  for lock contention on the exclusion constraint, before it turns into a
  user-visible timeout.
- **Free/busy query window (days requested)** — a heatmap over
  `freebusy_query_window_days_bucket`. Recorded *before* the 90-day cap
  (TECH-7) is enforced, so rejected oversized windows still show up — a
  cluster pressing against the cap is the signal to reconsider the limit,
  not just a `400` buried in the logs.
- **Outbox lag** — `outbox_lag`, seconds since the oldest unpublished
  `outbox_events` row was created. Near-zero and flat is healthy; a
  sustained climb means `OutboxPublisher` has stopped draining while writes
  keep landing — the primary signal that the outbox side of INFRA-3 is
  stuck.

Verify manually after starting the profile:

```bash
curl -s http://localhost:9090/api/v1/targets   # target for mini-doodle-app should be "health": "up"
```

then fire a few requests from §3 (a booking, a rebooking of the same slot
for a `conflict`, an availability query) and refresh the dashboard —
Grafana's default 1h window picks the traffic up on the next 15s scrape.

## 10. Testing

```bash
./gradlew test              # domain + application unit tests, no Docker
./gradlew integrationTest   # Testcontainers-backed, requires Docker running
```

- **Domain invariant unit tests** (`domain/src/test`) — `TimeSlot`
  transition guards, `Meeting.schedule()/cancel()` event recording,
  `AvailabilityAggregator.aggregate()` interval merging. Plain JUnit, no
  Spring, no database.
- **Use-case tests** (`application/src/test`) — each use case
  (`CreateBookingUseCase`, `CancelMeetingUseCase`, etc.) tested against
  in-memory fake repositories, verifying orchestration (slot + meeting +
  outbox writes in one operation) without a real database.
- **The mandatory concurrency test**
  (`infrastructure/src/integrationTest/.../BookingRaceIT`) — against a real
  `postgres:17` via Testcontainers, two scenarios:
  - 50 parallel `POST /bookings` on one slot, distinct requests: exactly
    one `201`, the rest `409`. This is the test that actually proves §5's
    exclusion-constraint claim rather than just asserting it in prose.
  - 50 parallel `POST /bookings` on one slot, all sharing the same
    `Idempotency-Key`: every `201` carries the same meeting id, and every
    `409` resolves to that same meeting on retry — proving `SCOPE-1`'s
    idempotency contract (retry/double-click of *one* caller, distinct
    from the exclusion constraint's job of separating *different*
    callers) under real concurrency, not just in a single-threaded test.

  Kept out of `./gradlew test`/`check`/`build` (Docker-dependent, slower)
  and run explicitly via `integrationTest`.
