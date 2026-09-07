# Reference Sources for DayLoop

Last reviewed: **25 August 2026**.

Use these as study/reference material. Prefer current official Android documentation for platform behaviour. Open-source repositories are useful for architectural ideas, edge cases and UX patterns, but their licences must be respected.

---

## A. Official Android / Google references — primary authority

### Material 3 in Jetpack Compose
**Why:** Primary UI foundation. Material 3 now includes Material 3 Expressive guidance and supports theming, dynamic colour, typography, shapes and adaptive layouts.

- https://developer.android.com/develop/ui/compose/designsystems/material3
- Compose Material 3 releases: https://developer.android.com/jetpack/androidx/releases/compose-material3

Snapshot when this file was written: the Android release page listed Material 3 stable `1.4.0` (12 Aug 2026). Do not freeze the project to this number indefinitely; use the current stable version when implementation begins.

### Jetpack Compose releases
**Why:** UI/runtime version awareness.

- https://developer.android.com/jetpack/androidx/releases/compose

Snapshot when reviewed: Compose groups such as runtime/UI/foundation listed stable `1.12.0` (12 Aug 2026).

### AlarmManager API
**Why:** Exact user-facing alarm scheduling, wake-up behaviour and exact-alarm capability checks.

- https://developer.android.com/reference/android/app/AlarmManager.html

Key items to study:
- `setExact`
- `setExactAndAllowWhileIdle`
- `setAlarmClock`
- `canScheduleExactAlarms`
- exact-alarm permission requirements
- one-shot scheduling for low-jitter repeated events

### Schedule alarms guidance
**Why:** Official recommendation on exact vs inexact alarms and permission choices.

- https://developer.android.com/develop/background-work/services/alarms

Important design lesson: exact alarms are intended for precise, user-facing functions; deferrable work belongs elsewhere.

### WorkManager
**Why:** Persistent deferrable work, constraints and recovery jobs. It is not an exact-alarm replacement.

- https://developer.android.com/reference/androidx/work/WorkManager

Use for maintenance/reconciliation/sync work that can run after constraints are met.

### Full-screen intent / urgent notification guidance
**Why:** Our alarm-grade surfaces must use this carefully and only for legitimate urgent alarm/calling use cases.

- https://developer.android.com/develop/ui/compose/notifications/create-notification
- https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,boolean)
- Android full-screen-intent limits: https://source.android.com/docs/core/permissions/fsi-limits

Important: on Android 14+, full-screen-intent permission is restricted; Play distribution may revoke it for apps that do not provide calling/alarm functionality. Build a fallback path.

### Room
**Why:** Canonical structured local task database.

Start from Android’s current Room documentation rather than tutorials copied from old versions:

- https://developer.android.com/training/data-storage/room

### DataStore
**Why:** Lightweight application preferences and configuration.

- https://developer.android.com/topic/libraries/architecture/datastore

---

## B. Native Android open-source references

### reReminder
Repository: https://github.com/ProfessorQuantumUniverse/reReminder

**Why it is especially relevant:**
- Kotlin;
- Jetpack Compose;
- Material 3 with dynamic colour;
- AlarmManager scheduling;
- multiple independent interval loops;
- weekday schedules;
- self-healing pattern in which the next alarm is armed as the loop progresses;
- offline/privacy-first design;
- no foreground polling service.

**What to inspect:**
- scheduling boundaries;
- one-shot recurrence/interval approach;
- alarm reconstruction;
- UI simplicity;
- settings model.

**Licence note:** GPL-3.0. Study the architecture and behaviour. Do not copy GPL code into a differently licensed product without understanding the consequences.

### WakeNote
Repository: https://github.com/MubarakNative/WakeNote

**Why:** Simple native Kotlin example combining AlarmManager, BroadcastReceiver, Room and Material components.

**What to inspect:**
- alarm CRUD lifecycle;
- Room ↔ scheduled alarm consistency;
- cancel-on-delete flow.

**Caution:** Treat it as a learning sample, not as authoritative modern Android guidance.

---

## C. Broader open-source product references

### AllisWell
Repository: https://github.com/mahirozdin/alliswell

**Why it is highly relevant:**
- alarm-grade reminders;
- re-alert-until-acknowledged concept;
- snooze presets;
- alarm diagnostics/logging philosophy;
- local-first architecture;
- recurring tasks;
- home-screen widgets;
- optional AI with guarded write behaviour;
- explicit architecture/ADR/agent documentation.

**What to inspect conceptually:**
- reminder escalation UX;
- how the project documents notification restrictions;
- offline mutation patterns;
- AGENTS/STATE/TASKS documentation workflow;
- AI proposal/consent boundary.

**Licence note:** The repository describes PolyForm Noncommercial licensing for its code. Do not treat it as permissively reusable commercial source.

### Adventure List
Repository: https://github.com/Merrit/adventure_list

**Why:** Cross-platform task app reference with due dates, recurring due dates, notifications and Android widget behaviour.

**What to inspect:**
- widget UX;
- recurring-task UX;
- syncing trade-offs.

**Technology:** Flutter, so use mainly for product/UX comparison rather than our Android-native architecture.

---

## D. Product UX references

These are not code sources; use them to study interaction design and identify gaps.

### Taskito
- https://www.taskito.io/

**Relevant patterns:**
- timeline/Agenda View;
- tasks and calendar events in one day view;
- drag-to-timeblock;
- recurring routines;
- widgets.

**DayLoop lesson:** borrow the clarity of a daily timeline without turning the home screen into a full calendar by default.

### TickTick
- Task duration help: https://help.ticktick.com/articles/7055782408586526720
- Week/timeline planning: https://help.ticktick.com/articles/7055782149730861056

**Relevant pattern:** a task can have a true start/end duration rather than a single due timestamp.

**DayLoop lesson:** store and render deadline and duration as different semantics.

---

## E. Design references to investigate during UI phase

Use Material 3 / Material 3 Expressive as the baseline, then study:

- Android system Clock/Alarm interaction patterns;
- Google Calendar’s time-block legibility;
- Taskito’s timeline density;
- TickTick’s duration editing;
- Android home-screen widget guidance;
- accessibility guidance for touch targets, contrast and TalkBack.

Avoid cloning the visual identity of any one product. The target identity is **Calm Pressure**: uncluttered, comfortable, progressively assertive when urgency increases.

---

## F. Source-selection hierarchy

When sources disagree, use this priority:

1. Current Android Developers / Android Open Source Project documentation.
2. Current Google Play policy documentation.
3. AndroidX API/release documentation.
4. Well-maintained open-source implementations.
5. Product help documentation.
6. Blog posts/tutorials.

Do not make alarm or permission decisions from a blog tutorial alone.
