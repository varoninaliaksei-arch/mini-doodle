# 0001. Modular monolith with layer-based Gradle modules

## Status
Accepted

## Context
The service has exactly one bounded context — availability and booking —
sharing a single invariant ("a slot can't be booked twice") and a single
transaction boundary. Splitting into multiple services would introduce a
saga where one Postgres commit is enough. Separately, the brief calls out
"clean architecture, sensible domain modelling" as an evaluation criterion,
and the main risk to that is Hibernate/Spring leaking into the domain
model.

## Decision
One deployable service, split into three Gradle modules by **layer**, not
by bounded context: `domain` → `application` → `infrastructure`. The
boundary is enforced structurally via `build.gradle.kts` dependency
declarations — `domain` has zero external dependencies, so a violation
fails to compile rather than failing a test that could be weakened or
deleted while the repo stays green (the alternative considered: a single
module plus ArchUnit).

## Consequences
Three build files and module wiring cost roughly half an hour up front;
retrofitting the split later would cost far more. If a second bounded
context appears, it lands in its own package (`ARCH-4`) and can split off
without a rewrite. Future service extraction, if it happens, follows
load/failure profile rather than domain nouns: `calendar-sync-service`
(I/O-bound, external rate limits) before `notification-service` (async,
event consumer); the transactional booking core stays a monolith longest.
