# Phase 11 — Product Quality, Accessibility and Battery Discipline

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Harden the deterministic product before optional intelligence.

## Prerequisites
- Core local product and Projects/Goals are functionally stable.

## Concepts to learn or reuse
- Room migrations (Level A at first real migration).
- Performance/battery profiling workflow (Level A/B).
- Accessibility audit workflow (Level B).

## Implementation slices
- Accessibility/TalkBack/large-text audit.
- Dark theme and visual review.
- Battery/performance/startup profiling.
- Notification channel migration checks.
- Database migration tests.
- Backup/export/import decision.

## Expected areas to change
- all core modules only as findings require
- migration tests
- performance/accessibility test assets

## Verification
- Migration paths.
- Accessibility checks.
- Startup/performance baseline.
- No new battery-polling regression.

## Definition of done
Core product is reliable and accessible enough that AI cannot hide foundational defects.

## Handoff
Next: **Phase 12 — Optional Planning Agent**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
