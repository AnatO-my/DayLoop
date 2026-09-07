# Phase 8 — Persistent and Strict Reminder Escalation

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Implement safe re-alert-until-resolved behaviour as a persisted one-shot state machine.

## Prerequisites
- Base alarm and recurrence paths are verified.

## Concepts to learn or reuse
- Persistent reminder state machine (Level A).
- Notification/alarm action idempotence (Level A/B).
- Lock-screen/privacy and channel behaviour (Level B).

## Implementation slices
- Persist escalation state.
- Schedule next one-shot re-alert.
- Complete/snooze/reschedule resolution paths.
- Apply strict snooze policy.
- Add privacy mode.
- Use alarm-grade surface only where eligible.

## Expected areas to change
- reminder/escalation domain
- core/alarms/
- core/notifications/
- settings
- tests

## Verification
- Chain survives process death.
- Resolution stops chain immediately.
- Duplicate actions are idempotent.
- Capability fallback works.

## Definition of done
A persistent task re-alerts according to policy after process death and stops immediately when resolved.

## Handoff
Next: **Phase 9 — Lifecycle Recovery and Diagnostics**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
