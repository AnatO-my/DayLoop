# ADR 0004: Android Version Policy

Status: Accepted for Phase 0 planning; re-check before first Play submission
Date: 2026-08-26
Phase: 0
Task: P0-04 Android version policy

## Context

DayLoop is an Android-first reminder and planning app. Its MVP depends on reliable local reminders, notification behavior, and later alarm-policy decisions. Android and Google Play version rules affect which devices can install the app, which platform behaviors the app must comply with, and which permission paths are available for time-sensitive reminders.

This decision covers only the Android version policy. It does not finalize exact-alarm permission strategy, full-screen intent usage, reminder UI behavior, or Play policy eligibility. Those remain assigned to P0-05 through P0-09.

## Decision

- Set `minSdk` to `26` for MVP.
- Set `targetSdk` to `36` for MVP and initial Google Play submission planning.
- Set `compileSdk` to `36` initially, aligned with `targetSdk` during Phase 1 project setup.
- Do not set `maxSdk`.
- Re-check `compileSdk` during Gradle/Compose setup if the current Android Gradle Plugin, Kotlin, Compose, or installed SDK tooling requires a newer compile SDK.

## Rationale

`minSdk 26` keeps DayLoop available to Android 8.0 and newer devices while starting from a platform generation that already has notification channels and stricter background execution behavior. That makes reminder reliability testing more realistic than supporting very old Android releases, while still preserving broad device support.

`targetSdk 36` is required for a new Android phone/tablet app or app update submitted through Google Play after the August 31, 2026 target API deadline. Targeting the current Play requirement early prevents DayLoop from building against obsolete behavior assumptions.

No `maxSdk` is set because DayLoop should remain installable on newer Android versions unless a future compatibility issue proves otherwise.

## Consequences

- Phase 1 Android project setup should configure `minSdk = 26`, `targetSdk = 36`, and initially `compileSdk = 36`.
- DayLoop must treat Android 12+ exact-alarm rules, Android 13+ notification and exact-alarm policy, and Android 14+ full-screen intent limits as first-class constraints.
- DayLoop should not assume privileged alarm capabilities are available. P0-05 and P0-09 must decide whether to request `SCHEDULE_EXACT_ALARM`, whether `USE_EXACT_ALARM` is policy-eligible, and what fallback behavior exists when exact alarms are unavailable.
- Test coverage before release should include at least API 26, API 31/32, API 33, API 34, API 35, and API 36 behavior, with at least one physical-device reminder test before public release.

## Deferred Decisions

- Exact alarm permission and fallback behavior.
- Full-screen reminder UI eligibility and fallback behavior.
- Final reminder acknowledgement, completion, and rollover semantics.
- Product name and application ID.

## Verification

P0-04 is verified when this ADR exists, `TASKS.md` marks P0-04 complete, `STATE.md` records the version policy, and the Phase 0 context manifest lists this ADR as relevant context for future Android setup.

## Sources Reviewed

- https://developer.android.com/google/play/requirements/target-sdk
- https://developer.android.com/guide/topics/manifest/uses-sdk-element.html
- https://developer.android.com/develop/background-work/services/alarms
- https://support.google.com/googleplay/android-developer/answer/16558241?hl=en&rd=1
