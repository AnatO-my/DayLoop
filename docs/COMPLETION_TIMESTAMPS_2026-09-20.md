# Completion Timestamp Handoff

## Scope and Approach
This slice fixes completion-date attribution, not recurring task instances or a permanent event history.
Previously Today and the idle calendar inferred completion dates from schedules. Late work was credited to the wrong day, unscheduled completions disappeared, and cross-midnight blocks could count on both days.

Task.completedAt is now a nullable Instant. Task.markCompleted(at) requires explicit time; callers pass their already-injected clock value. An already-completed task is unchanged, including legacy records whose date is unknown. Task.reopen() clears the timestamp. Editor copies retain it. Both app reopen callbacks now use reopen().

Room TaskEntity stores epoch milliseconds in completed_at; mapping is bidirectional. Database version 10 registers MIGRATION_9_10, which only adds a nullable INTEGER column. Existing rows/status/project associations remain intact. Do not backfill from deadlines or migration time. Do not use destructive migration.

TodayCompletedClassifier requires a ZoneId and uses completedAt rather than schedule. TodaySnapshotBuilder passes TodayMoment.zoneId; widgets already share that builder. Idle calendar projects the same timestamp into the device timezone. Focus history/streak logic and lifetime goal progress remain unchanged.

## Files
- core/model/Task.kt: completion and reopening transitions.
- core/database/{TaskEntity,DayLoopDatabase,DayLoopMigrations}.kt and di/DefaultAppGraph.kt: migration and registration.
- repository/TaskEntityMappers.kt: persistence conversion.
- repository/RoomAlarmStateStore.kt and core/alarms/AlarmNotificationActionProcessor.kt: completion writes.
- DayLoopApp.kt: reopening callbacks.
- today/{TodayCompletedClassifier,TodaySnapshotBuilder}.kt and feature/idle/IdleScreen.kt: date projections.
- Tests: TaskCompletionTimeTest, TodayCompletedClassifierTest, GeneratedTaskConnectionTest, CompletionMigrationTest and mechanical explicit-time updates to existing fixtures.

## Verification Status
Source inspected; scoped git diff --check passed. Wider worktree has unrelated existing whitespace warnings, left untouched. No Gradle, unit/instrumentation execution, emulator, installation or device operations were performed, following the user's manual-validation preference.

Schema 10 JSON has NOT been fabricated. Run the authorized Room compilation to export it, inspect and commit it. CompletionMigrationTest opens the exported v9 fixture and migrates using the current Room database implementation, validating the resulting schema through Room. It also checks null legacy dates, duplicate completion, edit preservation, reopen and recompletion round trips. Unit tests cover late/early dates, unscheduled/repeating tasks, midnight blocks, Lagos timezone, DST and legacy unknown dates. Tests remain unexecuted.

## Manual Acceptance
1. Upgrade a database containing active and completed tasks without clearing app data; check tasks and project links survive.
2. Complete yesterday's task today; verify Today credits today and retains that timestamp after editing.
3. Complete an unscheduled task; verify today's completion count. Repeat completion: time must not change.
4. Reopen and recomplete: the old timestamp clears and the new completion time is recorded.
5. Cross midnight and change timezone; dated summaries should use the current viewing timezone.

## Boundaries and Risks
- Legacy completed tasks have no reliable date and therefore no dated completion credit. Lifetime completed status is preserved.
- Reopening/deleting removes the record from completed summaries. A future immutable ledger is required to retain every historical event.
- Generated-task protocol versions can supersede old generated task state; recurring-instance redesign is still deferred.
- Existing partial backup/export support is not repaired here and must preserve timestamps in a dedicated follow-up.
- No automatic midnight refresh or new background polling was added. Existing refresh cadence still applies.
- Millisecond database precision is intentional. Timezone changes can move an Instant into a different calendar day.

Next single action: authorize/run the focused tests and migration check, generate schema 10, then manually accept the behaviour. Do not broaden this slice into recurring-task or widget feature work without direction.
