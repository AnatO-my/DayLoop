# Phase 13 — Calendar and Optional Sync

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Integrate external commitments only after local semantics are stable.

## Prerequisites
- Local task/alarm model is stable.

## Concepts to learn or reuse
- Calendar permission/data mapping (Level A when implemented).
- Offline sync/conflict principles (Level A if sync is included).

## Implementation slices
- Start with read-only calendar overlay.
- Add conflict detection.
- Add explicit event creation only if desired.
- Design account/sync/outbox/conflict model separately before implementation.

## Expected areas to change
- calendar integration package
- optional sync package
- settings/permissions
- tests

## Verification
- Calendar mapping.
- Permission denial.
- Conflict projection.
- Offline/outbox semantics if sync exists.

## Definition of done
External calendar data enriches planning without becoming a prerequisite for local reminders.

## Handoff
Next: **Phase 14 — Beta, Store Readiness and Field Reliability**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
