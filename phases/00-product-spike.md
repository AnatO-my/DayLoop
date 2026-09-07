# Phase 0 — Product Contract and Technical Spike

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Freeze the first coherent product contract and prove risky Android alarm behaviours before production architecture depends on them.

## Prerequisites
- Planning scaffold exists.
- No production code is assumed.

## Concepts to learn or reuse
- Android alarm capability model (Level A when the spike begins).
- PendingIntent/receiver/process-death basics (Level A).
- Policy research → ADR workflow (Level B; concept already introduced).

## Implementation slices
- Freeze product promise and non-goals.
- Write behavioural acceptance scenarios for task and reminder types.
- Choose minSdk/targetSdk using current Android requirements.
- Build exact-alarm/process-death capability spike.
- Compare normal high-priority notification with eligible alarm-style UI.
- Prove reboot reconstruction and one-shot interval scheduling.
- Record platform/policy decisions in ADRs.

## Expected areas to change
- docs/adr/
- temporary spike module/project when created
- STATE.md
- TASKS.md
- LEARNED_CONCEPTS.md

## Verification
- Exact alarm survives app process death.
- Capability denial is detected and has a fallback.
- Reboot reconstructs valid scheduled work.
- Interval spike schedules next one-shot event without polling.

## Definition of done
We can state exactly which reminder types require exact delivery, which Android capabilities are permitted/available, and what fallback occurs when privileged delivery is unavailable.

## Handoff
Next: **Phase 1 — Android Project Foundation**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
