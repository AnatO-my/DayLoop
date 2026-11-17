# DayLoop — functional audit and Rust migration handoff

Audited: **2026-11-08**. Target: reproduce the current Android app, then improve measured speed and reliability. This document describes existing behavior unless marked **proposed** or **gap**. It does not authorize silently changing product semantics.

## 1. Baseline and authority

- The **working tree**, including uncommitted files, is the baseline. HEAD at audit: `1a0d218f2f54041bdad2f4de4bf0b3573840cc11`; 218 changed/untracked status entries before this document. Preserve a complete snapshot before starting; cloning HEAD alone loses current functionality.
- Current implementation: Kotlin, Jetpack Compose/Material 3, Room/SQLite, Preferences DataStore, coroutines/Flow. There is no Rust implementation yet.
- Android application ID: `com.dayloop.app`; minimum API 26, compile/target API 36; JVM target 17. Dependency versions live in [app/build.gradle.kts](app/build.gradle.kts), [version catalog](gradle/libs.versions.toml) and the Gradle wrapper.
- Use source, exported schemas and tests as executable evidence. Historical plans are not proof of implementation. [AGENTS.md](AGENTS.md) and [accepted ADRs](docs/adr) explain existing constraints; the native-Kotlin architecture ADR will need a successor for Rust.
- This is a source audit, **not a fresh passing build/test report**. The November 6 notes report successful build/install and navigation checks, but not complete wardrobe workflows or the full regression suite. See [editorial handoff](docs/EDITORIAL_THEME_HANDOFF.md), whose newer follow-up supersedes its older sections.

## 2. Architecture and source map

Paths below are relative to `app/src/main/java/com/dayloop/app/` unless explicitly rooted elsewhere.

| Responsibility | Source of truth | Rust boundary |
|---|---|---|
| Startup, routing, overlays, task save wiring | `DayLoopApplication.kt`, `MainActivity.kt`, `DayLoopApp.kt` | Retain Android lifecycle adapter; port orchestration deliberately |
| Dependency composition | `di/AppGraph.kt`, `di/DefaultAppGraph.kt` | Explicit service/repository interfaces |
| Task, schedule, project, goal, clothing models | `core/model/` | Pure validated Rust types |
| Recurrence and project-generated work | `core/recurrence/`, `core/projects/` | Pure calculations with injected time/zone |
| Today classification and metrics | `today/`, `feature/today/TodayUiState.kt` | One shared snapshot engine for app/widgets |
| Canonical storage and serialization | `core/database/`, `repository/` | Initially Room adapter; later one SQLite owner |
| Alarm lifecycle and state transitions | `core/alarms/`, `repository/RoomAlarmStateStore.kt` | Rust state machine + Android scheduling adapter |
| Notifications, capabilities, recovery | `core/notifications/`, `core/reconciliation/` | Android services/receivers and permission UX |
| Pomodoro, sound, notification presentation | `feature/focus/` | Timer state engine; Android service/audio/OEM adapter |
| Pages and reusable UI | `feature/`, `ui/components/`, `ui/theme/` | Preserve UI contracts and assets |
| Preferences and walkthroughs | `settings/`, `ui/guides/`, `feature/onboarding/` | Separate versioned preference/guide state |
| Wardrobe, outfit records and image pipeline | `feature/wardrobe/`, `feature/outfit/`, `core/outfit/`, wardrobe/outfit repositories | Text/media store + image-processing interface |
| Encrypted wardrobe transfer | `core/backup/` | Byte-compatible codec or explicit version migration |
| Home widgets and picker previews | `widget/`, `app/src/main/res/xml/` | Keep native Glance/RemoteViews adapter |

Current data flow: UI intent → coordinator/repository → transactional persistence → platform side effects → observed Flow state → UI/widget snapshot. Do not let individual screens independently mutate alarm state.

## 3. Domain contracts that must survive translation

### Tasks and schedules

- IDs are **opaque nonempty strings**, not universally UUIDs. Generated tasks use deterministic IDs.
- A task contains title, description, status, priority, optional project, subtasks, optional schedule, reminder policy, provenance and optional completion instant. Preserve descriptions and subtask IDs through create/edit/serialization.
- Statuses: `ACTIVE`, `COMPLETED`, `SKIPPED`, `SOMEDAY`; priorities: `LOW`, `NORMAL`, `HIGH`. Completing an already-completed task preserves its timestamp. Reopen clears it.
- Schedule is an exclusive sum type: **none**, `AllDay(LocalDate)`, `Deadline(LocalDateTime)`, `TimeBlock(start,end)`, `Interval(rule)`, `CalendarRecurrence(rule, optional time)`. None and AllDay are different states. A block requires start < end.
- Reminder modes are `NORMAL`, `PERSISTENT`, `STRICT`, `FOCUS`. Task-level FOCUS requires a time block; it is **not** the standalone Pomodoro timer. Persistent/strict policies require a positive re-alert interval and positive cap. Snooze/reschedule permissions belong to the policy.
- Missed-work actions: keep/restore today → active AllDay(today); someday → unscheduled SOMEDAY; dismiss → SKIPPED. These clear completion time and normalize incompatible FOCUS policy. Review checks that the task has not changed since it was shown.

### Time and recurrence

- Preserve the distinction between civil dates/local date-times and absolute instants. Storage uses both; do not convert all dates to midnight UTC.
- `NextOccurrenceCalculator` converts local schedules using the system zone and Java `atZone`. Capture DST gap/overlap behavior in golden tests before choosing Rust time libraries.
- Deadline/block reminders use deadline/block start. None/AllDay and untimed calendar recurrence produce no alarm. An occurrence earlier than now is not newly calculated; equality is allowed.
- Calendar recurrence: daily/weekly/monthly/yearly, positive interval, start and optional inclusive end. Weeks anchor on Monday. Monthly/yearly dates **clamp** to the month's last valid day; they do not skip that month/year.
- Interval rules have a positive duration, eligible weekdays, date range, same-day active window and scheduled-start/completion anchor. Overnight windows are currently invalid. Port `IntervalNextEventCalculator` and its tests rather than approximating with repeating platform alarms.
- Today excludes recurring masters and merges generated instances with stored overrides **only when provenance matches**. Recurrence instance ID: `generated:recurrence:<master-id>:<date>`; provenance origin `RECURRENCE_RULE`, version currently 1. Generation materializes the requested current day, not an automatic historical backlog.
- Project instance ID: `generated:<project-id>:<date>`; provenance `PROJECT_RULE` / `SIMPLE_PHASE_CYCLE` / project version. Updating the protocol increments version. Preserve merge semantics so completion overrides are neither duplicated nor silently reused for a changed rule.

### Today, projects and goals

- Today sections: overdue, now, next, anytime, completed, missed. Read each `Today*Classifier` for exact boundaries and ordering. Overdue can overlap missed internally; display/count logic deduplicates by task ID.
- `total = distinct unresolved + completed`; empty day ratio is 0 and is **not** marked complete. Completed-today uses the completion instant in the viewing zone, not merely task status. Legacy rows without a completion time cannot be assigned a fabricated completion day.
- Project status: active/paused/completed/archived. Its optional simple phase cycle has anchor date, active/rest day counts and task template; only active projects on active days generate work.
- Goal status: active/completed/archived; policies: binary status, manual fraction [0,1], or count. COUNT currently counts supplied tasks **and projects** as work units; completed units / all units, zero when empty. The caller selects linked inputs. Do not replace this with a task-only ratio without a product decision.
- Tasks link to projects; projects optionally link to goals. There is no direct task-to-goal field in the current task schema.

## 4. Persistence and migration compatibility

Canonical database: **`dayloop.db`, Room schema 17**, explicit migrations 1→17, no destructive fallback. Exact SQL, nullability, foreign keys and indices: [schema 17](app/schemas/com.dayloop.app.core.database.DayLoopDatabase/17.json); conversion rules: entity mappers in `repository/`. Do not infer storage representation from domain types.

| Stored group | Tables / location | Critical detail |
|---|---|---|
| Tasks | `tasks` | Discriminated schedule columns; enum strings; subtasks JSON; provenance; description; completion timestamp |
| Reminder execution | `scheduled_occurrences`, `persistent_reminder_escalations`, `alarm_diagnostic_entries` | Persisted generations, statuses and epoch milliseconds |
| Planning | `projects`, `goals` | Protocol columns/version and goal policy/progress |
| Clothing/history | `clothing_items`, `outfit_records`, `outfit_items` | Outfit date identifies the daily record |
| Visual wardrobe | `wardrobe_media`, `clothing_item_media`, `outfit_media` | Media metadata and associations are distinct from files |
| Image bytes | app-private `files/wardrobe/` | Current repository resolves ID-based JPG/PNG files; preserve transparent output |
| App preferences | `files/datastore/dayloop.preferences_pb` | Includes profile, appearance, privacy, notes, icon mappings and guide-related preferences |
| Timer state | `files/datastore/focus.preferences_pb` | Independent durable timer state/history, not a Room task |

DataStore is not the canonical task store, but its existing notes/profile/timer data must not be discarded as “just preferences.” Migrate through a typed Kotlin export/import bridge initially; do not treat its protobuf files as JSON.

For an in-place upgrade preserve package ID, compatible signing identity, database and media paths, stable widget providers and notification/action identities. Choose **one writer per store**; never run independently migrating Room and Rust SQLite owners against the same database. If replacing Room, explicitly migrate its schema/version metadata and validate old installations before releasing.

### Existing transfer format

`BackupManager` exports **wardrobe details only**: clothing and linked item-based outfits; no complete app backup and no image bytes/photo-only outfits. Import rejects future outfit dates and validates the payload. Inspect `OutfitDao` for missing-record merge behavior.

Compatibility parameters from `BackupCipher`: `DAYLOOP-WARDROBE-1:` prefix, Base64 of salt(16) + nonce(12) + AES-256-GCM ciphertext/tag; PBKDF2-HMAC-SHA256, 600,000 iterations; 128-bit tag; prefix bytes as AAD. Password 12–256 characters, plaintext ≤5 MiB, file limit 8 MiB. Preserve password encoding through cross-language vectors. Do not relabel this as full backup or use it alone to migrate the app.

## 5. Platform execution contracts

### Task reminders

- `AlarmCoordinator` serializes mutations through `AlarmMutationLock`; `RoomAlarmStateStore` commits task/occurrence changes transactionally. Platform scheduling/cancellation follows the commit.
- Delivery validates persisted ID, generation and status; stale or repeated deliveries are normal no-ops. Occurrence states: scheduled/fired/cancelled. Snooze increments generation; current notification-action snooze is ten minutes. Complete/delete cancels related pending work and notifications.
- AndroidAlarmScheduler uses a stable PendingIntent identity and exact-and-allow-while-idle when permitted, inexact fallback otherwise; failures are diagnosed. Exact alarms and notification/full-screen permissions can be denied or revoked.
- Persistent reminders and interval work are persisted one-shot chains, not a polling service. Preserve caps, cancellation and stale action rejection.
- Recovery runs around boot, package replacement, clock/timezone and exact-alarm capability changes. Current `ScheduleReconciler` resubmits persisted scheduled instants, including past ones; it does **not** itself reinterpret every local rule in the new timezone. Define and test any improved timezone policy explicitly.
- Preserve permission/channel checks, privacy, strict/alarm-grade fallback and the repair UI. `ReminderActivity`, receiver registrations and manifest permissions remain Android responsibilities. WorkManager is not a replacement for precise user-facing alarms; AI/network must never be needed for delivery.

### Focus timers

- `FocusStore`: work/short/long defaults **25/5/15 minutes**; editable 1–180 minutes. Selected mode and active mode are separate. Running state stores an end instant; remaining seconds use ceiling rounding. Paused state retains remaining time and active mode.
- Completion credits only work sessions, in whole session minutes, to the **deadline's local day**; history retains the latest 366 dated entries. It increments sets and automatically starts the next break/work phase. Long break occurs every configured N sets (default 4, allowed 1–20). Delayed settlement starts the next phase at settlement time, not a replay of every missed cycle.
- Preserve pause/resume/reset/end/clear-sets distinctions, duration editing, sound preference and reflection. Current background sounds: none, rain, fire, stream, library, cafe.
- `FocusService` is a special-use foreground service. Notification remains ongoing while paused, with Resume and End. Commands carry state/revision tokens to reject stale actions.
- HyperOS support is isolated in `HyperOsFocusNotification` and presentation helpers, including OEM payloads/templates and mode-specific icons. Retain ordinary Android notification fallback. Global HyperOS 3 is a device-specific validation target, not a portable API guarantee. See [focus notification handoff](docs/FOCUS_NOTIFICATION_HANDOFF.md) and [HyperOS handoff](docs/HYPEROS_TIMER_HANDOFF_2026-09-20.md).

## 6. UI and secondary feature parity

| Surface | Required behavior / current direction |
|---|---|
| App shell | Today default; P&Gs and Focus always reachable. Add action only on Today/P&Gs. Respect status/navigation bars, IME and scroll padding. Do not duplicate headers or overlay owners. |
| Today | Compact greeting/profile name, progress strip, classified tasks, missed-work review, quick/full task editing. Saved daily outfit prompt disappears; calendar/wardrobe remains the review path, including photo-only records. |
| P&Gs | Projects/goals tabs; create/edit, icon selection, archive visibility, archive/delete actions, project tasks and goal progress; saved work restoration. |
| Focus | Timer modes and duration long-press, timer controls, background-sound picker, daily focused minutes. Repair controls belong in advanced settings. |
| Cover calendar | Downward gesture opens; upward/outside dismissal. Date hierarchy and nonoverlapping circles; outfit dates/details. No redundant idle-calendar button. |
| Advanced | Mascot/settings access, side panel, profile/display name, light/dark/system mode, typography/readability, guides, reminder repair/diagnostics, privacy and wardrobe transfer. Collapsed recent events with expand/clear. |
| Wardrobe | Adaptive photo grid, category filters, archive visibility, create/edit and media crop/import/link, archive/restore/delete confirmation. Text-only items remain valid. |
| Outfits | Dated clothing/photo associations, suggestion preview and explicit recording, calendar review. Current suggester picks random active items per category with injectable RNG; it does **not** rank by ML labels. |
| Guides | First-use/contextual walkthroughs with persisted progress and replay. One effective host; nested ownership must not render the same step twice. Preserve revision guards and accessibility. |
| Widgets | Today, Goal and Focus; privacy-aware summaries, deep links/actions and picker labels/previews. Reuse domain snapshots rather than duplicate counts. Match provider XML, Glance layout and preview behavior across supported Android versions. |
| Helpers | Notes, mascot gifts/usage, local assistant suggestions and natural-intent parser exist. These are not proof of a remote agent or trained clothes model. |

### Visual authority

Use current `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, components and bundled resources; not older matcha/purple screenshots. Current direction is neutral black/charcoal/light surfaces, **orange #FF6A24**, compact rectangular controls, differentiated navigation glyphs and clipped interaction feedback. Preserve touch targets and semantic labels even with icon-only visuals.

`AppFont.OCR_A` now means **OCR A BT**, bundled as `res/font/ocr_a.ttf`. `editorial_font` and `readable_body_text` are the relevant preference keys; readable body text defaults to JetBrains Mono while headings/labels use the selected family. Keep user overrides. Verify the supplied font's redistribution rights before shipping a new package; do not substitute a similarly named font silently.

The historical custom-color/matcha engine and enum names still exist in parts of the code. They are not authority to restore removed theme controls. Copy actual current resources and inspect both themes at large text sizes. [Editorial notes](docs/EDITORIAL_THEME_HANDOFF.md) and [visual contract](docs/DAYLOOP_VISUAL_CONTRACT.md) provide context; source wins where older notes differ.

### Clothes ML boundary

Implemented: manual visual/text storage plus `ClothingImageProcessor` using ML Kit generic subject segmentation and image labeling; segmentation may be unavailable, with fallback results. This is **not** a custom garment extraction model. Keep inference optional and separate from saving user-confirmed wardrobe data. Future stage: evaluated clothing model, confidence/correction UX and explicit storage/privacy decisions. See [visual wardrobe plan](docs/WARDROBE_VISUAL_STORAGE_IMPLEMENTATION_PLAN.md); planned work is not completed behavior.

## 7. Audit findings: address explicitly, do not reproduce as requirements

| Finding | Evidence / consequence | Migration treatment |
|---|---|---|
| Recurrence instances omit description/subtasks | `RecurrenceOccurrenceGenerator.kt` copies selected fields only; provenance version stays 1 | Define template-to-instance copying and edit/version policy; add tests before changing existing overrides |
| Project template reminder policy does not round-trip | `ProjectEntityMappers.kt` persists title/priority, rebuilds default policy | Add explicit storage migration or restrict template policy; do not silently claim full model persistence |
| Database and OS effects are not one transaction | Coordinator commits before AlarmManager operations; reconciliation resubmits persisted work | Add crash-injection tests; consider durable effect/outbox processing with idempotent acknowledgments |
| Several relationship tables lack secondary indices | Schema 17: `outfit_items`, `clothing_item_media`, `outfit_media` have no declared indices | Profile foreign-key/join paths; add appropriate indices through migration |
| Media pipeline masks failures | `ClothingImageProcessor` catches generic Exception and returns empty results | Preserve manual fallback but distinguish unavailable model, cancellation and failure; bound image memory and clean orphan files |
| Current export cannot recover the whole app | Backup scope excludes tasks/preferences/timers/media | Build and validate a complete migration path before replacing storage |
| Visual migration is not fully verified | Historical device checks cover navigation, not all typography/media flows | Capture current reference screens and validate font loading, large text, keyboard and all tabs |
| Rust speed/stability gains are unmeasured | No Rust baseline or benchmark comparison exists | Measure startup, frame time, DB latency, memory and timer battery cost; do not promise gains from language alone |

## 8. Proposed Rust structure and delivery sequence

**Recommended first boundary:** Rust domain/state engine with the existing Android shell. This minimizes parity risk; it is a proposal, not an existing implementation. A full Rust UI can follow only after Android accessibility, text input, lifecycle, widget and notification parity is demonstrated.

```text
crates/
  dayloop-domain/       # validated IDs, models, schedule types, pure state transitions
  dayloop-engine/       # Today, recurrence, protocols, progress, outfit selection
  dayloop-application/  # commands, repository/clock/platform ports, effect sequencing
  dayloop-storage/      # later SQLite ownership + migrations + media manifest
  dayloop-transfer/     # compatible wardrobe codec; separately versioned full migration
  dayloop-ffi/          # narrow Android boundary, DTOs, handles, error translation
android/               # Compose, Room bridge initially, services, alarms, widgets, ML Kit
fixtures/              # Kotlin-generated behavior/storage/notification golden vectors
```

Use immutable snapshots and typed commands/results. Inject clock, timezone, RNG and platform capabilities. Keep transaction ownership explicit; serialize competing timer/alarm mutations. UI callbacks must not run under storage locks. Use bounded workers for image/crypto work and propagate cancellation.

Keep JNI calls coarse-grained and avoid per-row/per-tick marshalling; this follows [Android JNI guidance](https://developer.android.com/ndk/guides/jni-tips). Specify buffer/handle ownership and callback lifetime. Do not unwind across JNI: translate expected failures into errors and contain unwinding panics at the boundary; `panic=abort` cannot be caught. See [Rust FFI guidance](https://doc.rust-lang.org/stable/nomicon/ffi.html).

1. **Freeze baseline:** archive working tree/assets and test fixtures; record schema/preferences/action identities and screenshots. Obtain a complete migration snapshot, not just wardrobe export.
2. **Port pure logic:** models, calendar/interval calculators, Today classifiers, project/goal calculations. Run Kotlin/Rust differential tests with identical input/time/zone/RNG fixtures.
3. **Integrate through ports:** retain Room/DataStore as sole writers and Kotlin Android adapters. Route commands through one coordinator; verify duplicate/stale events and process death.
4. **Move persistence only if justified:** validate migrations from all supported schemas, protobuf preference transfer, opaque/generated IDs, media paths and rollback. Switch writer ownership atomically.
5. **Optimize with evidence:** measure before/after on the same physical device/build conditions. Reduce redundant observation, parsing, image allocation and wakeups before rewriting UI for speculative gains.
6. **Release after parity:** preserve app data and Android identities; keep a tested rollback/version strategy. Separate intentional bug fixes from behavioral equivalence changes.

## 9. Verification gates and continuation checklist

Existing tests live under `app/src/test/` and `app/src/androidTest/`; port their behavioral expectations, not Kotlin implementation details. Starting commands: `./gradlew.bat :app:testDebugUnitTest :app:assembleDebug`; instrumentation: `./gradlew.bat :app:connectedDebugAndroidTest` on a dedicated test device. These commands were **not run during this audit**; do not clear the user's device data.

| Gate | Minimum evidence |
|---|---|
| Domain/time | Every schedule kind; midnight/DST/month end/leap year; completion-anchored interval; generated IDs/provenance; missed/overdue dedup; zero-task progress |
| Storage | Schema migration fixtures, description/subtask round-trip, project policy decision, completion dates, notes/profile, media associations and no destructive fallback |
| Delivery | Duplicate/stale commands, completion vs snooze race, crash between commit/effect, permission revocation, reboot/timezone, capped re-alerts |
| Timer | Pause/resume after process death, mode switch, duration edit, stale notification token, auto-cycle, delayed completion, cross-midnight credit, HyperOS + normal notification fallback |
| Wardrobe | Empty/text/photo-only records, cancel crop, archive/delete/restore, missing file, model unavailable, large image, password/tamper rejection and transfer scope |
| UI/platform | All three tabs, settings/guide single ownership, icon semantics/touch feedback, light/dark, large text/narrow screen/IME, saved outfit calendar access, widget preview/actions/privacy |
| Performance | Comparable cold start, frame latency, large-list/query latency, peak image memory, timer battery/wakeups; no regression hidden by a faster microbenchmark |

Resume by reading this file, `AGENTS.md`, the relevant source group and tests. Resolve the findings table before claiming exact replication. Keep old plans and styling history as references; do not port obsolete screens, duplicate guide hosts or speculative ML controls just because they appear in prior notes.
