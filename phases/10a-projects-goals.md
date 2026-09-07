# Phase 10A — Projects, Goals and Behavioural Protocols

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Add long-horizon planning and active/rest protocols without cluttering Today.

## Prerequisites
- Core task/reminder system is mature.
- ADR 0003 reviewed before implementation.

## Concepts to learn or reuse
- Task → Project → Goal implementation boundary (Level B; concept already taught).
- Anchor-based Active/Rest phase evaluation (Level A).
- Derived occurrence provenance/versioning (Level A).

## Implementation slices
- Accept/refine ADR 0003.
- Implement Goal/Project models + persistence.
- Link tasks and provenance.
- Create Plans screen.
- Implement deterministic ProjectProtocol with Active/Rest phases.
- Implement ProjectOccurrenceGenerator.
- Verify 3-study/3-rest reference cycle.
- Define protocol edit/anchor reconciliation and Goal progress.

## Expected areas to change
- core/projects/
- project/goal persistence
- feature/plans/
- Today provenance integration
- tests

## Verification
- Active day generates expected task.
- Rest day generates no obligation.
- Rest does not block DAY COMPLETE.
- 3→3 cycle boundary.
- Pause differs from Rest.
- Protocol edit invalidates stale generation.

## Definition of done
A study/rest Project can generate only appropriate active-day tasks and relate them to a Goal while Today remains execution-first.

## Handoff
Next: **Phase 11 — Quality, Accessibility and Battery Discipline**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
