# DayLoop Functionality Guides

Date: 2026-09-21
Status: Coverage draft. Guide infrastructure and first-launch/Today delivery exist; introduction replay is available from More. Remaining coverage and runtime validation are pending.

This is the content and coverage checkpoint, not a claim that every feature guide is complete. Runtime behavior still needs manual confirmation; current implementation updates are recorded in STATE.md.

## 1. First Launch

Start the day opens Today. Today holds daily work; P&Gs holds projects and goals; Focus holds work and break timers. The plus above the navigation bar creates a task on Today or a planning item on P&Gs.

First-launch walkthrough sequence: identify Today, identify the three destinations, highlight the task creation action, then let the user choose whether to create a task. Do not force a sample task, request permissions immediately or block navigation until a task exists.

Evidence: `feature/onboarding/WelcomeScreen.kt`, `DayLoopApp.kt`.

## 2. Today

The Completed and Remaining summary cards open their corresponding task lists. Tap a task to edit it. Scroll to reach all daily sections; do not treat a partly visible task above the navigation dock as the end of the list.

Task gestures:
- Swipe left and release past the action threshold to complete an eligible task.
- Swipe right to open timing choices; this does not immediately snooze the task.
- Hold a task for about two seconds to open its options.
- Snooze is available only when a reminder is active and the task permits it. The current menu offers ten minutes.
- Rescheduling opens the editor and may be disabled by the task's reminder policy.

Offer the corresponding accessibility actions instead of requiring swipes or a timed hold. Do not force the user to complete real work just to finish a tutorial.

Evidence: `feature/today/TodayScreen.kt`, `TaskGestureRow.kt`, `TaskActionsDialog.kt`.

## 3. Create and Edit Tasks

Enter a title, add an optional description, and add subtasks as needed. Use the options button beside Description to open Task Settings. Save task changes to persist the draft; closing the screen is not a save action.

Task Settings contains schedule, priority, project association and reminder behavior. Choose a schedule before filling its relevant fields:
- On a date: put the task on a planning date without a specific time.
- Deadline: choose when the task is due.
- Time block: reserve a start and end time.
- Interval: configure recurring alerts within an eligible window.
- Repeat: configure a calendar recurrence rule.

Validation messages must be resolved before saving. Reminder behavior and scheduling are separate choices. A deadline does not itself override notification permissions or reminder policy.

Descriptions now have database storage. Text discarded by older versions cannot be restored automatically.

Evidence: `feature/taskeditor/TaskEditorScreen.kt`, `TaskSettingsFields.kt`, `TaskEditorDraft.kt`; description persistence audit in STATE.md.

Scheduling copy audit (2026-09-21): the obsolete generation warnings have been removed from TaskSettingsFields. Device validation of recurrence remains pending. Do not advertise unlimited historical recurrence backfill or individual interval-instance tracking.

## 4. Missed and Saved Work

Open Missed work from Today when available. Choose Keep for today, Reschedule, Someday or Dismiss for each item. Dismiss is not the same as permanently deleting the task.

In P&Gs, open Saved work. Switch between Someday and Dismissed, then choose Bring to today or Reschedule. Do not imply missed work is automatically completed or permanently removed.

Evidence: `feature/today/MissedTaskReviewScreen.kt`, `feature/plans/PlansScreen.kt`.

## 5. Projects and Goals

Open P&Gs and select Projects or Goals. Use the plus to create an item in the selected context. Tap an existing card to edit it. Hold a card to open its management options. Show archived reveals archived items.

Explain project/task association at the point where a project is selected in Task Settings. Explain goal progress in a separate guide after verifying the selected progress mode; do not present all goals as automatically task-derived.

Archive and permanent deletion need distinct demonstrations. Never delete an existing project or goal automatically as a tutorial step.

Evidence: `feature/plans/PlansScreen.kt`, `PlanningEditors.kt`.

## 6. Focus and Break Timers

Select Pomodoro, Short break or Long break, then start the selected timer. Use Pause and the available resume/start action to continue it. Reset session resets the selected session. Clear sets is a separate action from timer reset.

Hold a timer mode to edit its duration; the timer display also has a duration-edit action. Work duration settings include sets until a long break. Saving defaults should not be described as rewriting an already running session.

Open Background sound to choose a sound or Off. Focused time today reports accumulated focus time, not the remaining countdown. Session reflection is separate from the countdown and can be skipped.

Evidence: `feature/focus/FocusScreen.kt`, `FocusSoundPicker.kt`; duration and HyperOS handoffs referenced by STATE.md.

Device-specific notification guide: distinguish app controls from operating-system presentation. Do not promise that every device displays Xiaomi's timer island. Check permissions and actual device behavior before demonstrating paused notification resume.

## 7. Wardrobe Items

Open Wardrobe from Today's outfit section. Enter an item name, select a category and use plus to add it. Use the item's photo action to select an image. Archive removes the item from new suggestions while preserving its record; restore makes it available again. Delete requires confirmation.

The current visual-storage implementation imports pictures into app-managed storage. Do not describe this as automatic garment extraction or promise a public Gallery album called DayLoop Outfits. AI extraction remains deferred.

Evidence: `feature/wardrobe/WardrobeScreen.kt`; visual storage stages recorded in STATE.md.

## 8. Outfit Suggestions and History

Suggest creates a preview; Wear this confirms it. Cancelling a suggestion must not record it as worn. Record opens the outfit editor for the selected day. Select wardrobe items, optionally add full-outfit photos, and save. Removing a photo from the editor selection is not a promise that the original image is deleted.

Calendar day selection opens that day's outfit editor. Dates with attached photos have visual indicators. Photo selection and saving the outfit are separate steps.

Evidence: `feature/today/TodayScreen.kt`, `feature/outfit/OutfitDetailDialog.kt`, `feature/idle/IdleScreen.kt`.

## 9. Calendar Cover

Swipe downward beginning in the app's title area to open the cover calendar. This is not the system notification shade gesture. Swipe upward, tap the outside dismissal area, or use Back to exit. An accessibility action also opens the calendar.

Tap a valid calendar day to access its outfit record. Blank calendar cells are not selectable dates. The cover is unavailable on Welcome and while the host disables it for busy operations.

Evidence: `feature/idle/IdleCalendarHost.kt`, `IdleScreen.kt`.

## 10. More, Personalization and Notes

Tap or hold the mascot in a main-screen header to open More. The current route is the mascot action, not an assumed left-swipe gesture. Use the close control or Back to dismiss it.

Routine shared icon buttons have hold-to-show labels. This does not apply to every long press: task rows, project cards and timer modes have their own actions.

Notes supports entering text, adding it with plus and deleting an existing note. Do not promise undo or a deletion confirmation where none is implemented.

Personalization checklist: verify display-name editing, supported fonts, font weight, reduced motion, theme mode and privacy controls against their current UI before publishing individual instructions. Do not reintroduce retired palette controls in the guide.

Evidence: `ui/components/DayLoopPageHeader.kt`, `DayLoopComponents.kt`, `feature/settings/NotesScreen.kt`, `AdvancedSettingsScreen.kt`, `feature/diagnostics/DiagnosticsScreen.kt`.

## 11. Reminder Troubleshooting and Events

Open More to inspect capabilities and Advanced reminder settings. Repair reminders is a recovery action, not a requirement before every timer. Recent Events starts collapsed; expand it to inspect events and use Clear to clear that history.

Permission guides must open settings only after a deliberate user action, preserve refusal as a supported choice, and recheck capability after returning. Do not equate a notification permission with permission for exact alarms.

Evidence: `feature/diagnostics/DiagnosticsScreen.kt`, `DayLoopApp.kt`.

## 12. Widgets, Backup and Other Secondary Features

These need dedicated short guides, not extra first-launch slides:
- Today, Goals and Focus widgets: add through the launcher's widget picker; confirm current sizing, labels, privacy behavior and actions on the user's launcher before finalizing instructions.
- Backup: the current More screen exposes Create Encrypted Backup. Verify the user-visible restore entry, password handling and inclusion of photo files before promising full recovery. Do not infer photo backup support from wardrobe metadata backup.
- Efficiency Report: explain observed values only after reviewing its current calculations; avoid battery-life guarantees.
- Mascot gifts/assistant: audit current reachable controls and distinguish suggestions from completed actions before writing final guidance.

These are coverage entries awaiting their own targeted source and manual audits. They are not verified walkthroughs.

## Guide Coverage Checklist

- [x] First-launch and primary navigation outline.
- [x] Today summaries, task gestures and contextual menu constraints.
- [x] Task editor, description and schedule entry paths.
- [x] Missed work and Saved work.
- [x] Projects/Goals basic entry and management actions.
- [x] Focus modes, duration editing and background sound.
- [x] Wardrobe photos, outfit preview/confirmation and calendar entry.
- [x] More entry, routine tooltips, Notes and reminder diagnostics.
- [ ] Verify recurrence copy and progress-mode explanations.
- [ ] Audit personalization, widgets, backup/restore, efficiency and assistant details.
- [x] Implement shared presentation, progress persistence and introduction replay.
- [ ] Complete other guide flows and their individual replay entries; verify live behavior.
- [ ] Manually validate all guide actions on a real build.
