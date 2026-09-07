# Context Manifest — Phase 04

**Phase brief:** `../phases/04-today-ui.md`

## Always read
- `../STATE.md`
- `../phases/04-today-ui.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- TodaySnapshot/API files
- `../app/src/main/java/com/dayloop/app/MainActivity.kt`
- `../app/src/main/java/com/dayloop/app/feature/today/TodayUiState.kt`
- `../app/src/main/java/com/dayloop/app/feature/today/TodayScreen.kt`
- `../app/src/test/java/com/dayloop/app/feature/today/TodayUiStateTest.kt`
- `../app/src/androidTest/java/com/dayloop/app/MainActivityTest.kt`
- `../app/src/androidTest/java/com/dayloop/app/feature/today/TodayScreenTest.kt`
- core/design source

## Read only if a specific decision blocks work
- FEATURE_SPEC.md Today UX only

## Do not load or modify by default
- alarm internals
- recurrence internals
- Projects/Goals/AI

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
