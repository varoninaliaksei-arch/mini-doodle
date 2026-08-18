# 0004. No Redis, Kafka, API gateway, or Elasticsearch in the base profile

## Status
Accepted

## Context
Target scale is hundreds of users and ~10⁴ slot rows, with bookings rare
and free/busy reads dominating (read:write ≳ 20:1). At this scale, adding a
broker or cache to the base `docker-compose.yml` doesn't demonstrate an
ability to think about scale — it reads as an inability to estimate load
against the stated numbers.

## Decision
The base profile is exactly two containers: `app` + `postgres:17`. Redis,
Kafka, an API gateway, and Elasticsearch are all deliberately excluded,
each with a stated re-introduction trigger rather than a blanket "not
needed":

| Component | Verdict | Reconsider when |
|---|---|---|
| Kafka | Excluded | A real second consumer of domain events appears (the transactional outbox, `0003`, is the connection point) |
| Redis (cache) | Excluded | Postgres read latency stops meeting SLA at higher row counts |
| Redis (locks) | Excluded, structurally | Never — strictly weaker guarantee than the exclusion constraint (`0002`) |
| API gateway | Excluded | A second service exists to route to |
| Elasticsearch | Excluded | Full-text search enters scope |

## Consequences
Every excluded component has a named threshold, so adding it later is a
documented decision, not a scramble. The `observability` (Prometheus +
Grafana) and `scale` (nginx + 2 replicas) compose profiles are opt-in
additions on top of this base, not exceptions to it.
