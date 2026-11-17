# DayLoop System Audit & Architecture Expansion

**Date**: 2026-11-10  
**Version**: 2.0  
**Author**: Senior Android Architect & Development Team  
**Target Platform**: Android (Kotlin, Jetpack Compose, Room, DataStore, AlarmManager, CalendarContract)

---

## 1. System Executive Summary

DayLoop is an offline-first, deterministic daily focus and task execution system for Android. It bridges micro-level time management (Tasks, Reminders, Interval Alarms, Focus Timers) with macro-level intent (Projects, Goals, Routine Outfits, Notes).

### Core Architectural Guarantees
1. **Deterministic Execution**: User alarms and reminders are scheduled directly via Android `AlarmManager` and execute regardless of app process status or network availability.
2. **Offline-First Persistence**: Room database is the single canonical source of truth for Tasks, Occurrences, Projects, Goals, Wardrobe/Outfits, and Notes.
3. **Reactive UI Stream**: Jetpack Compose renders UI driven strictly by immutable StateFlows emitted from ViewModels and Repositories.
4. **Time Invalidation & Timezone Hygiene**: All temporal calculations use explicit time providers (`AppClock`, `TimeZoneProvider`) allowing robust unit testing and timezone handling.

---

## 2. Layer & Module Architecture

```text
┌────────────────────────────────────────────────────────────────────────┐
│                              Compose UI                                │
│ Today • Plans/Goals • Focus • Notes • Wardrobe • Settings • Idle • Editor│
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                             ViewModels                                 │
│ TodayViewModel • TaskEditorViewModel • NotesViewModel • FocusViewModel │
│ CalendarSyncViewModel • PlansViewModel                                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
┌───────────────────────────────────▼────────────────────────────────────┐
│                     Domain & Repositories                              │
│ TaskRepository • NoteRepository • CalendarSyncRepository               │
│ OutfitRepository • WardrobeMediaRepository • SettingsRepository        │
└───────────────┬───────────────────┬────────────────────┬───────────────┘
                │                   │                    │
┌───────────────▼────────┐  ┌───────▼─────────────┐  ┌───▼──────────────┐
│       Room DB          │  │  DataStore Prefs    │  │ Android Platform │
│ Tasks, Occurrences,    │  │ Theme, Appearance,  │  │ CalendarContract │
│ Notes (v21), Outfits,  │  │ Timer Defaults,     │  │ AlarmManager     │
│ Goals, Projects        │  │ Feature Flags       │  │ Notifications    │
└────────────────────────┘  └─────────────────────┘  └──────────────────┘
```

---

## 3. Commercial Productivity App Parity & Audit

To compare DayLoop with top-tier commercial productivity applications (e.g. *TickTick*, *Notion*, *Todoist*, *Google Tasks*), we audited every existing feature surface and identified structural gaps to be upgraded:

| Feature Area | Current DayLoop Implementation | Commercial Standard (e.g. TickTick / Notion) | Upgrade Strategy |
| :--- | :--- | :--- | :--- |
| **Note Taking** | Basic `Note` strings in DataStore preferences. No title, no rich text, no tags, no task-linking. | Full Markdown/rich-text notes, standalone note hub, tags, pinned notes, note-to-task conversion. | **Phase 1 Upgrade**: Re-architect into Room `NoteEntity` (v21), `NoteDao`, `NoteRepository`, `NoteEditorScreen`, and upgraded `NotesScreen`. |
| **Calendar Sync** | Internal `IdleCalendarHost` visual dot calendar. No connection to Google Calendar or device calendars. | 2-way sync with Google Calendar/Outlook, timeline overlay of external events, export tasks as calendar events. | **Phase 2 Upgrade**: Integrate Android `CalendarContract` provider API, permission flow, `CalendarSyncRepository`, and overlay on Today & Idle screens. |
| **Task Engine** | Schedules (Date, Deadline, Time Block, Interval, Recurrence), Subtasks, Prioritization, Missed Work review. | High parity achieved. Subtask checkboxes, recurrence instances (Skipped/Completed). | Maintained and integrated with Notes and Calendar export. |
| **Focus Engine** | Pomodoro, Short/Long Break, Xiaomi HyperOS timer notifications, haptic heartbeat, ambient sounds. | High parity achieved. | Integrated with external Calendar event timelines. |
| **Wardrobe & Outfits** | On-device ML Kit clothing recognition, outfit recording, photo storage, daily ensemble suggestions. | Unique DayLoop differentiator. | Parity established. |

---

## 4. Deep Audit of Upgraded Systems

### 4.1 Note-Taking Infrastructure (Room v21)
- **Entity**: `NoteEntity` (`id`, `title`, `content`, `is_pinned`, `is_archived`, `linked_task_id`, `linked_project_id`, `created_at`, `updated_at`, `tags`).
- **DAO**: Reactive `Flow<List<NoteEntity>>`, search filter using `LIKE` pattern on title/content/tags, ordering by pinned status and timestamp.
- **Data Migration**: Non-destructive `MIGRATION_20_21` creates table `notes` and transfers any legacy notes from DataStore preferences into Room.

### 4.2 Google & System Calendar Sync Engine (`CalendarContract`)
- **Android System Bridge**: `CalendarContractManager` reads calendars from `CalendarContract.Calendars` and events from `CalendarContract.Events` / `CalendarContract.Instances`.
- **Permissions**: Safe runtime checks for `android.permission.READ_CALENDAR` and `android.permission.WRITE_CALENDAR`.
- **Calendar Event Domain**: `CalendarEvent` (`id`, `calendarId`, `title`, `description`, `startTimestamp`, `endTimestamp`, `isAllDay`, `location`, `color`).
- **Today & Idle View Integration**: External calendar events are overlaid directly into Today's daily timeline alongside DayLoop tasks, avoiding schedule conflicts.

---

## 5. Security, Permissions, & Failure Governance

1. **Calendar Permission Denial**: When calendar permissions are denied, the app degrades gracefully: internal task execution continues without disruption, and an unobtrusive permission request banner is shown in `CalendarSyncSettingsScreen`.
2. **Database Integrity**: All Room migrations are strictly non-destructive. Version bumps are verified with Room schema verification tests.
3. **Data Privacy**: External calendar data read via `CalendarContract` is stored in memory or transient cached Flow state only; external credentials or calendar data are never uploaded to remote servers.
