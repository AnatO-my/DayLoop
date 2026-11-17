# Missed-Work Review Handoff

## Request and Starting Point
User selected the Android Studio implementation_plan.artifact.md titled "Missed-task review". Work continued its existing partial source, including SOMEDAY/SKIPPED, missed classification, a review screen and preliminary routing. Existing completion timestamp and recurrence-instance changes were preserved. The plan is design context, not authority to run device date changes or builds; the user's manual-validation preference remains in force.

## Behaviour
- Active all-day tasks dated before today, deadlines strictly before now, and time blocks at/after their end need review. Deadline-at-now stays Next; block-at-end enters review with no visibility gap.
- Unscheduled Inbox tasks, recurrence/interval masters, completed, Someday and skipped tasks do not enter review.
- Review has Keep for today, Reschedule, Someday and Dismiss. Keep uses today's AllDay date. Someday clears schedule; Dismiss persists SKIPPED without deletion or completion credit.
- Focus-only reminder policy becomes Normal when removing its required time block. Other policies and project/provenance identity are preserved.
- P&Gs > Saved work offers Someday and Dismissed lists. Bring to today restores ACTIVE + today's AllDay schedule. Reschedule opens the existing editor and saving reactivates the task. Cancelling preserves its saved state. Back from the editor returns to its originating list.

## Data Flow and Counting
TodayMissedTaskClassifier feeds TodaySnapshotBuilder. Snapshot.missed is the complete review collection, including overdue deadlines. Snapshot.overdue retains those deadlines. itemsIn(MISSED) excludes overdue IDs so each list row appears once. Snapshot.unresolved deduplicates the combined work; missed work therefore prevents false empty/complete summaries. TodayUiState and the widget mapper expose the same counts. Review is derived, not another canonical database state.

DayLoopApp resolves generated project and current recurrence instances for edit/complete/actions. Review acts on the snapshot task, not only database rows, so a virtual instance can be persisted as an override. The recurrence master is not changed. Widget completion resolution includes current recurrence instances too.

AlarmCoordinator.reviewTask reads current stored state under AlarmMutationLock, rejects stale expected tasks, rechecks review eligibility against its injected clock, then calls the private lock-owned save path. Existing RoomAlarmStateStore cancellation invalidates generations and resolves reminder escalation chains transactionally; the coordinator cancels scheduled alarms/notifications afterward. Busy UI disables repeated controls. The public save method is not called recursively under the lock.

## Persistence
Statuses are already stored as names in a TEXT column, so no new migration is required for SOMEDAY/SKIPPED. Do not increment schema or fabricate exports for this slice. Current database version is 11 due to the separate recurrence work. CompletionMigrationTest now registers both 9->10 and 10->11 when opening the current database. Downgrading to an older app that does not know these enum values is not supported.

## Files
- Task.kt and TaskReviewAction.kt: transitions, action vocabulary and Focus normalization.
- TodayMissedTaskClassifier.kt, TodaySnapshot.kt, TodaySnapshotBuilder.kt: boundaries/counts.
- TodayUiState.kt, TodayScreen.kt, MissedTaskReviewScreen.kt: banner, review and Saved work UI.
- AlarmCoordinator.kt: serialized review mutation and shared save internals.
- DayLoopApp.kt, PlansScreen.kt: route/editor/recovery connections.
- TodayWidgetMapper.kt, TodayWidget.kt: counts and generated completion resolution.
- Tests: TodayMissedTaskClassifierTest, TaskReviewTransitionTest, MissedTaskReviewScreenTest; updated TodaySnapshotTest, TodayUiStateTest, AlarmResolutionPersistenceTest and CompletionMigrationTest.

## Verification
Focused source review and scoped git diff --check passed. Tests were authored, not executed. No Gradle, emulator, device, install/uninstall, clock changes or app-data operations ran. Existing unrelated worktree changes and whitespace were preserved.

Manual acceptance after the user's build:
1. Use date pickers to schedule one all-day task yesterday, one deadline earlier today and one ended block. No device-clock change is required.
2. Verify the banner, correct count, Overdue visibility, missed rows and matching remaining/widget totals.
3. Keep a Focus-policy block for today; confirm it becomes an all-day task with no old reminder.
4. Move one task to Someday; reopen P&Gs > Saved work after restart, restore or reschedule it.
5. Dismiss a task; it must not count as completed, and must remain recoverable in Dismissed.
6. Reschedule to a future date; cancel once (no mutation), save once (leaves review), and test system/header Back.
7. Test rapid taps and changes from another task surface; a stale review must not overwrite a newer decision.
8. Check large text, small screens, TalkBack, light/dark themes and reminder cancellation on a real device.

## Known Boundaries
The existing recurrence/project generators do not backfill all historical unsaved occurrences. This slice handles persisted past tasks and the current generated projection; it must not be described as complete historical recurrence recovery. Series-versus-instance alarm ownership also needs separate acceptance against the existing recurrence implementation. Interval missed-occurrence history is not inferred from a master task.

Global editor concurrency and the existing partial backup system are not redesigned. Latest completion timestamps are not an immutable audit log. Saved-work sorting currently follows repository order. Next action is manual acceptance plus authorized focused tests, not automatic expansion into backfill or other features.
