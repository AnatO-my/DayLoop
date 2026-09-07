# Phase 9 — Lifecycle Recovery and Diagnostics

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Make schedule loss/restriction visible, explainable, and repairable.

## Prerequisites
- Alarm/escalation engine is stable.

## Concepts to learn or reuse
- Boot/time/timezone/package-replaced reconciliation (Level A).
- Capability/permission repair flow (Level B).

## Implementation slices
- Register lifecycle recovery events.
- Rebuild valid schedules from Room.
- Handle permission/capability state.
- Create diagnostics projection/screen.
- Add manual repair action.

## Expected areas to change
- reconciliation package
- Android receivers/manifest
- feature/diagnostics/
- tests

## Verification
- Reboot.
- Time/timezone change.
- Package update.
- Permission revoked/restored.
- Repair is idempotent.

## Definition of done
The app can explain expected-vs-actual delivery path and reconstruct valid schedules.

## Handoff
Next: **Phase 10 — Home-Screen Widget**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
