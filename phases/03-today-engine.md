# Phase 3 — Today Query Engine

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Make Today a deterministic domain capability rather than a Compose filter.

## Prerequisites
- Task domain/persistence exit gate passed.

## Concepts to learn or reuse
- Derived domain snapshots/use cases (Level B).
- Flow composition for time-dependent state (Level B/A if Flow not yet taught).
- Day-boundary/time-zone reasoning (Level A).

## Implementation slices
- Create TodaySnapshot model/use case.
- Classify overdue/now/next/anytime/completed.
- Calculate day progress and OPEN/COMPLETE.
- Observe via Flow.
- Define midnight/empty-day behaviour.

## Expected areas to change
- today/domain or usecase package
- task repository queries
- today unit tests

## Verification
- Active-now time block.
- Overdue deadline.
- Cross-midnight task.
- Completion removes unresolved obligation.
- Empty day semantics.

## Definition of done
Given fake time and fixtures, the exact Today model is produced without Compose.

## Handoff
Next: **Phase 4 — Today UI / Calm Pressure**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
