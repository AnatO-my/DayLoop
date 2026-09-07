# Context Manifest - Phase 03

**Phase brief:** `../phases/03-today-engine.md`

## Always read
- `../STATE.md`
- `../phases/03-today-engine.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- task repository/domain files:
  - `../app/src/main/java/com/dayloop/app/core/model/Task.kt`
  - `../app/src/main/java/com/dayloop/app/core/model/TaskSchedule.kt`
  - `../app/src/main/java/com/dayloop/app/core/model/RepeatRules.kt`
  - `../app/src/main/java/com/dayloop/app/repository/TaskRepository.kt`
- Today use-case/tests:
  - `../app/src/main/java/com/dayloop/app/today/TodaySnapshot.kt`
  - `../app/src/main/java/com/dayloop/app/today/TodayMoment.kt`
  - `../app/src/main/java/com/dayloop/app/today/TodayOverdueClassifier.kt`
  - `../app/src/main/java/com/dayloop/app/today/TodayNowNextClassifier.kt`
  - `../app/src/main/java/com/dayloop/app/today/TodayAnytimeClassifier.kt`
  - `../app/src/main/java/com/dayloop/app/today/TodayCompletedClassifier.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodaySnapshotTest.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodayBoundaryTest.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodayOverdueClassifierTest.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodayNowNextClassifierTest.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodayAnytimeClassifierTest.kt`
  - `../app/src/test/java/com/dayloop/app/today/TodayCompletedClassifierTest.kt`

## Read only if a specific decision blocks work
- FEATURE_SPEC.md Today behaviour only
- ARCHITECTURE.md Today data-flow section only

## Do not load or modify by default
- Compose UI internals
- alarm platform adapters
- Projects/Goals/AI

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
