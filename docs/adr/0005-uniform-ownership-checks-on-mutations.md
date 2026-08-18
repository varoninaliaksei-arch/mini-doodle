# 0005. Ownership checks apply uniformly to every mutation, not just creation

## Status
Accepted

## Context
`X-User-Id` is a stub identity header (no JWT/OAuth — out of scope for this
assignment). At the time `POST /slots`, `POST /slots/bulk`, and
`POST /bookings` were built, the header was used to stamp ownership on the
new row (`calendarId`/`organizerId`), but nothing checked it against the
*existing* owner on the mutation endpoints added afterwards —
`PATCH /slots/{id}`, `DELETE /slots/{id}`, and `POST /meetings/{id}/cancel`
all accepted a resource id alone and mutated whatever they found, callable
by anyone who knew the UUID. That's inconsistent with how the same header is
already enforced on creation, not a considered simplification of it.

## Decision
The same `X-User-Id` check that gates creation now gates every mutation.
Each mutating use case loads the resource, compares the caller's
`X-User-Id` against the resource's own recorded owner (`TimeSlot.calendarId()`
for slot mutations — including the new `block`/`unblock` operations —
`Meeting`'s `organizerId` for cancellation, since cancelling a meeting is an
action on the Meeting aggregate and should check that aggregate's own owner
reference), and returns `403` via a new `NotOwnerException` if they don't
match.

*Alternative considered:* leave mutation endpoints unchecked, matching the
"no full auth, out of scope" framing already applied to `X-User-Id` as a
whole. Rejected — an identity stub that's enforced on some writes and not
others is a weaker Stage-2 position to defend than either extreme
("no auth anywhere, by design" or "auth stub enforced everywhere it can be").
Uniform enforcement costs a few lines per use case and keeps the story
simple: the stub's scope is "no real authentication," not "authentication
enforced inconsistently."

## Consequences
Every mutating use case now takes a `callerId` parameter and every mutating
endpoint requires the `X-User-Id` header (already true for creation; now
also true for update/delete/block/unblock/cancel). `GET /slots` and
`GET /availability` are unaffected — reading someone else's calendar was
always allowed (`ownerId` is an explicit query parameter there, not an
identity check). A real auth system would replace `NotOwnerException`'s
equality check with a proper authorization decision; the stub's shape
already matches where that would plug in.
