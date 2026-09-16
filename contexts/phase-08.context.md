# Context Manifest — Phase 08

**Phase brief:** `../phases/08-escalation.md`

## Always read
- `../STATE.md`
- `../phases/08-escalation.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- reminder policy/state files
- core/alarms
- core/notifications
- settings + escalation tests
- `../app/src/main/java/com/dayloop/app/repository/PersistentReminderEscalationRepository.kt`
- `../app/src/main/java/com/dayloop/app/repository/RoomPersistentReminderEscalationRepository.kt`
- `../app/src/main/java/com/dayloop/app/repository/PersistentReminderEscalationEntityMappers.kt`
- `../app/src/main/java/com/dayloop/app/repository/ScheduledOccurrenceRepository.kt`
- `../app/src/main/java/com/dayloop/app/repository/ScheduledOccurrenceEntityMappers.kt`
- `../app/src/main/java/com/dayloop/app/repository/TaskRepository.kt`
- `../app/src/main/java/com/dayloop/app/core/database/DayLoopDatabase.kt`
- `../app/src/main/java/com/dayloop/app/core/database/PersistentReminderEscalationDao.kt`
- `../app/src/main/java/com/dayloop/app/core/database/ScheduledOccurrenceDao.kt`
- `../app/src/main/java/com/dayloop/app/di/DefaultAppGraph.kt`
- `../app/src/androidTest/java/com/dayloop/app/core/database/EscalationSchedulingPersistenceTest.kt`

## Read only if a specific decision blocks work
- LOOPS.md escalation section only
- SOURCES.md Android notification/full-screen refs only if current behaviour must be verified

## Do not load or modify by default
- Projects/Goals
- widget
- AI/calendar

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
