# ADR 0005: Alarm Capability and Reminder Policy

Status: Accepted for Phase 0 planning; re-check before first Play submission
Date: 2026-08-27
Phase: 0
Task: P0-10 ADR update

## Context

DayLoop is a today-first Android task execution app whose MVP depends on trustworthy local reminders. Phase 0 proved the key Android behaviours before production architecture begins:

- P0-05 verified exact-alarm capability checks, degraded fallback, stale-delivery validation, and ordinary process-death delivery on API 36.
- P0-06 verified notification permission/channel checks, high-priority notification behaviour, full-screen intent capability checks, and full-screen denial fallback on API 36.
- P0-07 verified reboot reconstruction from persisted scheduled occurrence state on API 36.
- P0-08 verified interval reminders as persisted one-shot chains without polling on API 36.
- P0-09 reviewed current Android and Google Play constraints for exact alarms and full-screen intents.

The product contract requires deterministic local reminders, explicit reminder strictness, platform honesty, and no dependence on AI, network, foreground polling, or WorkManager as an exact-reminder substitute.

## Decision

DayLoop's MVP alarm/reminder capability strategy is:

- Use `SCHEDULE_EXACT_ALARM` as the MVP exact-alarm permission direction.
- Do not declare `USE_EXACT_ALARM` for MVP.
- Treat exact-alarm access as a revocable/degraded capability, not a guaranteed entitlement.
- Check `AlarmManager.canScheduleExactAlarms()` before exact scheduling on Android 12/API 31 and higher.
- Use exact alarms only for explicit user-facing reminder occurrences that genuinely require precise timing.
- Use inexact alarm fallback for user-facing reminders when exact access is unavailable and useful.
- Do not use WorkManager, JobScheduler, foreground services, or polling loops as if they were exact user-facing alarm delivery.
- Persist scheduled occurrence state outside `AlarmManager`; receivers reload canonical state and reject stale deliveries before doing user-visible work.
- Reconstruct active valid scheduled work after boot and after exact-alarm permission changes from persisted state.
- Implement interval reminders as one-shot chains: persist the rule plus current occurrence, schedule only the next event, and advance only after valid delivery or explicit resolution.
- Declare/use full-screen intent only for explicit user-selected alarm-grade/strict reminder surfaces where the product and Play policy position remain defensible.
- Check `NotificationManager.canUseFullScreenIntent()` before claiming alarm-style full-screen capability on Android 14/API 34 and higher.
- Fall back to high-importance alarm/reminder notifications when full-screen intent is unavailable, denied, stripped, or visually unreliable.
- Always check notification permission and channel state before posting reminders on Android versions where they apply.
- Record degraded delivery/capability states for diagnostics and repair UX.
- Never claim that Android cannot override, delay, silence, downgrade, deny, or revoke reminder behaviour.

## Reminder Eligibility Matrix

| Reminder/task surface | Exact alarm eligibility | Full-screen eligibility | MVP direction |
|---|---|---|---|
| Normal reminder | Usually no; exact only if the user configured a precise clock reminder and capability is granted. | No. | Use notification/inexact fallback by default. |
| Persistent reminder | Possible for user-facing re-alert occurrences when precision matters and capability is granted. | Usually no. | Persist one-shot chain; stop on completion/snooze/reschedule/cap. |
| Strict reminder | Yes, when explicitly user-selected and precision matters. | Possible, but only after explicit opt-in and capability checks. | Candidate for strongest MVP reminder contract with honest fallback. |
| Focus prompt | Usually no. | No. | Use normal/high-priority notification prompts tied to time-block state. |
| Alarm-grade user-selected reminder | Strongest exact-alarm candidate. | Strongest full-screen candidate. | Must remain opt-in, policy-conscious, capability-checked, and fallback-backed. |
| Interval task/reminder | Exactness depends on reminder contract, not interval timing alone. | No default full-screen UI. | Always one-shot chain; never polling or repeating alarm. |
| Calendar recurrence | Exactness depends on reminder contract and occurrence. | No default full-screen UI. | Expand next occurrence deterministically, then schedule according to reminder policy. |
| Background repair/reconciliation | No. | No. | Use app-start reconciliation, boot/package/time receivers, or deferrable work where appropriate. |
| AI/planning work | No. | No. | AI may propose only; deterministic app code owns scheduling. |

## Required Fallback Behaviour

When exact-alarm access is unavailable, DayLoop must:

- persist the scheduled occurrence and degraded reason;
- avoid exact scheduling calls that would crash or mislead;
- use the best permitted inexact alarm path for user-facing reminders where useful;
- surface a repair path only when exact timing is important to the selected reminder contract;
- keep stale-delivery validation and cancellation semantics intact.

When full-screen-intent access is unavailable, DayLoop must:

- post a high-importance notification fallback when notification permission/channel state allows;
- preserve meaningful actions such as Complete, Snooze, Reschedule, and Open;
- record the degraded state for diagnostics;
- route users to system settings only after clear in-app explanation;
- avoid promising that every eligible alarm will visually take over the screen on every device state.

When notification permission, notification enablement, or channel importance is unavailable/degraded, DayLoop must:

- record diagnostics;
- preserve canonical task/reminder state;
- expose a repair path;
- avoid silently marking reminders complete or delivered.

## Consequences

### Positive

- Keeps the MVP aligned with current Android and Google Play constraints.
- Preserves deterministic local reminder behaviour without AI/network dependence.
- Gives production code a clear permission/capability boundary before architecture begins.
- Keeps user trust by treating degraded platform state as normal and visible.
- Allows exact/alarm-grade behaviour for genuinely user-selected high-cost reminders without making every reminder privileged.

### Costs

- Some reminders will be less precise when exact-alarm access is denied.
- DayLoop needs diagnostics and repair UX earlier than a simple task app would.
- Full-screen alarm UI must be implemented and tested conservatively.
- Store-release preparation must include Play Console declaration review and policy re-check.
- Physical-device/OEM behaviour still requires manual testing beyond the API 36 emulator spikes.

## Future Implementation Requirements

Phase 1 should create the Android Studio project without declaring `USE_EXACT_ALARM`.

The alarm engine phases must include:

- scheduled occurrence persistence in Room;
- stable `PendingIntent` identity using occurrence id and generation metadata;
- stale-event guard in every delivery/action receiver;
- exact capability checker abstraction;
- fallback scheduling path;
- boot/package/time/timezone/permission reconciliation;
- diagnostics log for denied, degraded, stale, and fallback events;
- notification permission/channel/full-screen capability checks;
- physical-device alarm UI verification before shipping alarm-grade surfaces.

Phase 7/8 must decide and test:

- interval anchor semantics: clock-anchored vs acknowledgement-anchored;
- persistent reminder acknowledgement semantics;
- strict reminder snooze/reschedule limits;
- maximum re-alert horizon/caps.

Before first Play submission, re-check:

- target/compile SDK requirements;
- exact-alarm policy and Play declarations;
- full-screen intent policy and Play declarations;
- whether product positioning still supports any alarm-grade or full-screen claims.

## Deferred Decisions

- Product name and application ID.
- Final all-day rollover policy.
- Final acknowledgement semantics for Persistent reminders.
- Final snooze presets and maximum escalation horizon.
- Physical-device/OEM acceptance matrix.

## Verification

P0-10 is verified when:

- this ADR exists;
- `TASKS.md` marks P0-10 complete;
- `STATE.md` records Phase 0 as complete and points to Phase 1 project foundation as the next single action;
- the Phase 0 context manifest lists this ADR;
- `LEARNED_CONCEPTS.md` records the Phase 0 capability strategy as verified.

## Sources and Evidence

- `docs/spikes/p0-05-exact-alarm-spike.md`
- `docs/spikes/p0-06-alarm-ui-spike.md`
- `docs/spikes/p0-07-reboot-recovery-spike.md`
- `docs/spikes/p0-08-interval-one-shot-spike.md`
- `docs/spikes/p0-09-play-policy-review.md`
- Android alarm scheduling guide: https://developer.android.com/develop/background-work/services/alarms
- Android `AlarmManager` reference: https://developer.android.com/reference/android/app/AlarmManager
- Android `PendingIntent` reference: https://developer.android.com/reference/android/app/PendingIntent
- Android notification permission guide: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android notification channels guide: https://developer.android.com/develop/ui/compose/notifications/channels
- Android `NotificationManager.canUseFullScreenIntent()` reference: https://developer.android.com/reference/android/app/NotificationManager#canUseFullScreenIntent()
- Android `Notification.Builder.setFullScreenIntent()` reference: https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,boolean)
- Google Play sensitive permissions / exact alarm policy: https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1
- Google Play foreground service and full-screen intent requirements: https://support.google.com/googleplay/android-developer/answer/13392821?hl=en
- AOSP full-screen intent limits: https://source.android.com/docs/core/permissions/fsi-limits
