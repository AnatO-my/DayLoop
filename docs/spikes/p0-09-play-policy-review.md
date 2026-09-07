# P0-09 Play Policy Review

Date: 2026-08-27
Status: Verified against current official Android / Google Play sources
Task: P0-09 Policy review

## Question

Which exact-alarm and full-screen-intent capabilities can DayLoop responsibly design around for MVP, given current Android platform behaviour and Google Play policy?

## Current Official Constraints

### Exact alarms

Android exact alarms are for precise, user-facing, time-critical work. Android recommends inexact alarms whenever precision is not required, and WorkManager/JobScheduler for deferrable work.

For Android 12/API 31 and higher, apps that set exact alarms with `PendingIntent` must declare an Alarms & reminders permission. For Android 13/API 33 and higher, the app can declare either:

- `SCHEDULE_EXACT_ALARM`: user/special-access grant, broader use cases, revocable by user or system, denied by default for most fresh installs targeting Android 13+.
- `USE_EXACT_ALARM`: automatically granted and not user-revocable, but limited to narrow qualifying use cases and subject to Google Play policy.

If `SCHEDULE_EXACT_ALARM` is revoked, Android stops the app and cancels future exact alarms. DayLoop must therefore check `canScheduleExactAlarms()` before exact scheduling and reschedule from persisted state after permission grant, similar to boot recovery.

### Google Play exact alarm policy

Google Play treats privileged alarms/reminders APIs as sensitive. The practical MVP implication is that DayLoop should not assume it qualifies for the auto-granted `USE_EXACT_ALARM` permission merely because it has reminders. DayLoop is currently positioned as a today-first task execution app, not a dedicated alarm clock or calendar app.

`USE_EXACT_ALARM` should remain out of MVP unless P0-10 deliberately changes product positioning and records why DayLoop's core functionality qualifies. The safer MVP default is `SCHEDULE_EXACT_ALARM` with explicit user-facing explanation and a degraded fallback path.

### Full-screen intents

For Android 14/API 34 and higher, `USE_FULL_SCREEN_INTENT` is restricted. Google Play default-enables it only for apps whose core functionality is setting alarms or receiving phone/video calls. Other apps can still use full-screen intents only after user grant, and must degrade gracefully when denied.

Android provides `NotificationManager.canUseFullScreenIntent()` for runtime checks and `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` to send the user to the system settings page. If full-screen access is denied, Android presents a heads-up style fallback instead of launching the full-screen surface.

## DayLoop MVP Policy Direction for P0-10

- Use `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM`, as the default MVP exact-alarm permission direction.
- Treat exact-alarm access as optional/degraded platform capability, not guaranteed app entitlement.
- Exact delivery may be used only for explicit user-facing reminder occurrences that need precise timing, especially user-selected alarm-grade commitments and possibly strict/persistent reminder chains when the product contract says precision matters.
- Normal reminders, focus prompts, background reconciliation, diagnostics, sync, cleanup, and AI/planning work must not require exact alarms.
- Interval reminders must remain one-shot chains. Whether each interval occurrence qualifies for exact delivery depends on the reminder contract and P0-10 ADR, not merely on being an interval task.
- Do not rely on Play default full-screen-intent grant for MVP. DayLoop is not currently a dedicated alarm-clock or calling app.
- Alarm-style/full-screen UI must be opt-in, user-selected, capability-checked, and backed by high-importance notification fallback.
- The app must never claim that Android cannot override reminders. Permission denial, channel downgrade, full-screen denial, DND, OEM variance, battery restrictions, and user settings are normal degraded states.

## Reminder Contract Mapping

| Contract | Exact alarm direction | Full-screen direction | Notes |
|---|---|---|---|
| Normal | Prefer inexact unless the user configured an exact clock reminder and capability is granted. | No full-screen UI. | High-importance notification only when warranted by user settings/channel. |
| Persistent | One-shot re-alert chain; exact only when precision is product-critical and capability is granted. | Usually no full-screen UI. | Must stop on completion/snooze/reschedule/cap. |
| Strict | Candidate for exact delivery when user explicitly opts in. | Candidate for alarm-style UI only when user explicitly opts in and capability is granted. | Needs honest fallback and constrained snooze/reschedule policy. |
| Focus | Prefer normal/high-priority notifications tied to time-block prompts. | No default full-screen UI. | A time block is not automatically an alarm. |
| Alarm-grade user-selected | Strongest candidate for exact delivery. | Candidate for full-screen request, but no Play default-grant assumption. | Must satisfy policy, declaration, capability check, and user-control requirements. |
| Interval | One-shot chain only. Exactness depends on reminder contract. | No default full-screen UI. | Do not use Android repeating alarm or polling. |
| Background repair/reconciliation | No exact alarm. | No full-screen UI. | Use WorkManager/JobScheduler or app-start reconciliation where appropriate. |

## Required Fallbacks

If exact-alarm access is unavailable:

- persist the occurrence and degraded delivery reason;
- use the best permitted inexact alarm path for user-facing reminders where useful;
- show a repair path only when exact timing is genuinely needed;
- never schedule via WorkManager as if it were exact delivery.

If full-screen-intent access is unavailable:

- post a high-importance alarm/reminder notification where notification permission and channel settings allow;
- record the degraded state for diagnostics;
- send the user to settings only after clear in-app explanation;
- preserve Complete, Snooze, Reschedule, and Open actions where possible.

If notification permission or channel importance is denied/degraded:

- record diagnostics;
- keep canonical task/reminder state intact;
- expose a repair path;
- do not crash or silently mark reminders delivered.

## Sources Reviewed

- Android alarm scheduling guide: https://developer.android.com/develop/background-work/services/alarms
- Google Play sensitive permissions / exact alarm policy: https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1
- Google Play foreground service and full-screen intent requirements: https://support.google.com/googleplay/android-developer/answer/13392821?hl=en
- AOSP full-screen intent limits: https://source.android.com/docs/core/permissions/fsi-limits
- Android `NotificationManager.canUseFullScreenIntent()` reference: https://developer.android.com/reference/android/app/NotificationManager#canUseFullScreenIntent()
- Android `Notification.Builder.setFullScreenIntent()` reference: https://developer.android.com/reference/android/app/Notification.Builder#setFullScreenIntent(android.app.PendingIntent,boolean)

## P0-10 ADR Inputs

P0-10 should record the final capability strategy as an ADR:

- exact-alarm permission: likely `SCHEDULE_EXACT_ALARM`, not `USE_EXACT_ALARM`;
- exact delivery eligibility by reminder contract;
- full-screen intent declaration/use strategy;
- required fallback and diagnostics behaviour;
- Play Console declaration implications before store release;
- future physical-device/OEM test matrix.
