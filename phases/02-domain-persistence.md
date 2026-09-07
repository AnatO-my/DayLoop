# Phase 2 — Domain Model and Persistence

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Represent task semantics and local persistence correctly before rich UI.

## Prerequisites
- Foundation builds cleanly.
- Fake clock/time conventions exist.

## Concepts to learn or reuse
- Immutable Kotlin domain models (Level A/B).
- Sealed schedule hierarchy (Level A).
- Room entity/DAO/domain separation (Level A).
- Repository boundary and Flow (Level A).

## Implementation slices
- Implement identifiers/status/priority.
- Implement TaskSchedule variants.
- Implement ReminderPolicy and recurrence/interval models.
- Add Room schema/DAOs and mappers.
- Add repository interfaces/implementation.
- Add DataStore only for settings/preferences.

## Expected areas to change
- core/model/
- core/database/
- repository package
- settings/DataStore package
- unit tests

## Verification
- Schedule validation.
- Persistence round-trip.
- Completion idempotence.
- Invalid time ranges.
- Date-boundary fixtures.

## Definition of done
Every MVP task type can be created, stored, read, edited and completed without UI-specific logic.

## Handoff
Next: **Phase 3 — Today Query Engine**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
