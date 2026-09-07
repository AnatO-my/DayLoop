# Context Manifest — Phase 02

**Phase brief:** `../phases/02-domain-persistence.md`

## Always read
- `../STATE.md`
- `../phases/02-domain-persistence.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- Gradle files only when persistence dependency wiring is relevant:
  - `../gradle/libs.versions.toml`
  - `../build.gradle.kts`
  - `../app/build.gradle.kts`
- core model/database source + tests as created:
  - `../app/src/main/java/com/dayloop/app/core/database/DayLoopDatabase.kt`
  - `../app/src/main/java/com/dayloop/app/core/database/TaskDao.kt`
  - `../app/src/main/java/com/dayloop/app/core/database/TaskEntity.kt`
  - `../app/src/main/java/com/dayloop/app/repository/TaskRepository.kt`
  - `../app/src/main/java/com/dayloop/app/repository/RoomTaskRepository.kt`
  - `../app/src/main/java/com/dayloop/app/repository/TaskEntityMappers.kt`
  - `../app/src/main/java/com/dayloop/app/settings/AppSettings.kt`
  - `../app/src/main/java/com/dayloop/app/settings/SettingsRepository.kt`
  - `../app/src/main/java/com/dayloop/app/settings/DataStoreSettingsRepository.kt`
  - `../app/src/test/java/com/dayloop/app/settings/DataStoreSettingsRepositoryTest.kt`
  - `../app/src/test/java/com/dayloop/app/repository/RoomTaskRepositoryTest.kt`
  - `../app/src/test/java/com/dayloop/app/repository/PersistenceValidationTest.kt`
  - `../app/src/androidTest/java/com/dayloop/app/core/database/TaskDaoTest.kt`
  - `../app/schemas/com.dayloop.app.core.database.DayLoopDatabase/1.json`
  - `../app/src/main/java/com/dayloop/app/core/model/Task.kt`
  - `../app/src/main/java/com/dayloop/app/core/model/TaskSchedule.kt`
  - `../app/src/main/java/com/dayloop/app/core/model/ReminderPolicy.kt`
  - `../app/src/main/java/com/dayloop/app/core/model/RepeatRules.kt`
  - `../app/src/test/java/com/dayloop/app/core/model/TaskTest.kt`
  - `../app/src/test/java/com/dayloop/app/core/model/TaskScheduleTest.kt`
  - `../app/src/test/java/com/dayloop/app/core/model/ReminderPolicyTest.kt`
  - `../app/src/test/java/com/dayloop/app/core/model/RepeatRulesTest.kt`
- ADR 0002 only for deterministic-core boundary

## Read only if a specific decision blocks work
- FEATURE_SPEC.md task types only if semantics are unclear
- PRODUCT_CONTRACT.md task/status/priority snippets only if semantics are unclear
- ARCHITECTURE.md domain/persistence sections only

## Do not load or modify by default
- Today UI
- widget
- AI
- calendar
- Projects/Goals implementation

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.






