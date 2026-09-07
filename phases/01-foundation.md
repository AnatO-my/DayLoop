# Phase 1 — Android Project Foundation

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Create the smallest maintainable Android Studio foundation and verification baseline.

## Prerequisites
- Phase 0 exit gate passed.
- Platform/permission assumptions recorded.

## Concepts to learn or reuse
- Android Studio/Gradle project structure (Level A).
- Coroutines basics (Level A if not previously taught).
- Time-provider/fake-clock pattern (Level A).
- Material 3 project theme baseline (Level B; deep design comes later).

## Implementation slices
- Create Kotlin + Compose project.
- Add Material 3, version catalog and agreed dependency setup.
- Establish package/module boundaries without premature over-modularisation.
- Add DI if accepted.
- Create injectable time abstraction.
- Add unit/instrumentation test baseline and CI verification.

## Expected areas to change
- settings.gradle(.kts)
- build.gradle(.kts)
- gradle/
- app/
- core/time/ or equivalent
- test configuration

## Verification
- Fresh-clone build.
- Sample Compose screen renders.
- Fake clock unit test passes.
- CI/lint/unit baseline passes.

## Definition of done
A fresh clone builds and tests with Compose, DI/time abstractions, and no product feature logic required.

## Handoff
Next: **Phase 2 — Domain Model and Persistence**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
