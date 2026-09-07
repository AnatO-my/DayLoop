# Phase 6 — Scheduler and Exact Alarm Engine

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Build reliable, reconstructible Android alarm scheduling.

## Prerequisites
- Phase 0 alarm findings accepted.
- Task persistence and editor exist.

## Concepts to learn or reuse
- AlarmManager (Level A unless fully taught by spike).
- PendingIntent identity (Level A).
- BroadcastReceiver lifecycle (Level A).
- Stale-event reconciliation (Level A).

## Implementation slices
- Persist ScheduledOccurrence.
- Implement next-occurrence calculation.
- Define AlarmScheduler interface.
- Implement Android AlarmManager adapter.
- Implement receiver that reloads source-of-truth state.
- Implement cancel/reschedule and capability fallback.
- Add diagnostics record.

## Expected areas to change
- core/alarms/
- core/notifications/
- database occurrence tables
- AndroidManifest.xml
- alarm tests

## Verification
- Stale/duplicate/completed/rescheduled event.
- Process death.
- Permission absent.
- Cancellation identity.

## Definition of done
Scheduled tasks fire without keeping the process alive and stale delivery cannot resurrect obsolete work.

## Handoff
Next: **Phase 7 — Interval and Recurrence Engine**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
