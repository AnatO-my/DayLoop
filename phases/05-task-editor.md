# Phase 5 — Task Creation and Time Semantics

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Make task entry fast while preserving explicit time semantics.

## Prerequisites
- Domain schedule types exist.
- Today UI can render created tasks.

## Concepts to learn or reuse
- Compose form state/validation (Level B).
- Progressive disclosure for schedule complexity (Level B).

## Implementation slices
- Quick add.
- Full editor.
- Schedule type selection.
- Deadline/time-block/interval/recurrence editors.
- Reminder contract editor.
- Separate importance from reminder strictness.

## Expected areas to change
- feature/taskeditor/
- domain validation adapters
- UI tests

## Verification
- Every schedule type maps to valid domain state.
- Invalid ranges show useful errors.
- Editor round-trips existing task.

## Definition of done
All domain schedule types can be created/edited without ambiguous date fields.

## Handoff
Next: **Phase 6 — Scheduler and Exact Alarm Engine**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
