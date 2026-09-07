# Phase 7 — Interval and Recurrence Engine

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Implement repeat behaviour with deterministic next-event calculations and no polling.

## Prerequisites
- Alarm scheduling pipeline verified.

## Concepts to learn or reuse
- Calendar recurrence algorithms (Level A).
- Interval anchor/window semantics (Level A).
- DST/time-zone edge cases (Level A/B).

## Implementation slices
- Interval next-event engine.
- Active-window/eligible-day handling.
- Self-scheduling next one-shot interval event.
- Calendar recurrence expansion.
- Missed-occurrence and short-month rules.
- Recurrence preview/edit scope if in MVP.

## Expected areas to change
- core/recurrence/
- core/time/
- scheduler integration
- recurrence tests

## Verification
- 31st/short month.
- Leap year.
- Weekday interval.
- Window close.
- Timezone/DST.
- Reboot after missed interval.

## Definition of done
Intervals and recurrences behave predictably across calendar edge cases without background polling.

## Handoff
Next: **Phase 8 — Persistent and Strict Reminder Escalation**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
