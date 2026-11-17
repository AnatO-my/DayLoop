# Contextual Guides Implementation Plan

Date: 2026-09-21
Status: Progress persistence, first-launch/Today delivery and More-header introduction replay implemented; latest hardening is not build/device-verified. Other replay entries remain pending. See GUIDE_DELIVERY_HANDOFF.md.

## Objective

Introduce first-launch orientation and first-use guidance without forcing users through every feature. Use DAYLOOP_FUNCTIONALITY_GUIDES.md as the audited coverage source. Treat unverified sections as release gates, not finished content.

## Current Baseline

- Welcome exists and onboardingCompleted is persisted through SettingsRepository.
- Shared icon tooltips already explain routine icon actions.
- GuideProgress and GuideProgressPreferences provide versioned progress through SettingsRepository. The first-launch/Today host is connected, and More has an introduction-replay icon. A guide-selection menu remains pending.
- Existing tasks, reminders, wardrobe media and app settings must remain unchanged by guide dismissal or replay.

## Proposed Delivery Sequence

1. Complete this source-audit/content checkpoint.
2. Define and test a versioned guide-progress model independent of onboardingCompleted.
3. Implement first-launch and Today presentation as one bounded slice.
4. Extend to task editor and scheduling after reconciling stale recurrence copy.
5. Add P&Gs, Focus, wardrobe/outfits and calendar guides individually.
6. Audit and add secondary guides plus a replay entry.

## State and Persistence Contract

Use stable guide IDs and explicit versions, not visible text or list indices. Suggested IDs: first_launch, today, task_editor, scheduling, missed_work, plans, focus, wardrobe, outfits, calendar, settings, widgets, troubleshooting, backup.

Distinguish unseen, in_progress, skipped and completed. A skip suppresses automatic redisplay for that guide version but remains replayable. Completion of Welcome is not completion of all guides. Existing installations should not be sent back to Welcome or receive every new guide at once.

Store progress in preferences, not task tables. Persist meaningful transitions, not animation frames or pointer movement. Handle preference-write failure without falsely marking completion. Keep unknown future IDs when editing known progress. Invalid stored progress must not crash app launch.

## Eligibility and Interruptions

- Read settings before deciding whether to launch orientation.
- Show guidance only on its relevant screen and when its actual control exists and is visible.
- Suppress automatic guidance during save operations, modal editors, permission requests, urgent notification entry and active gestures.
- Incoming task/widget routes take precedence; do not navigate away to deliver orientation.
- Recompute target bounds after scrolling, rotation, insets and text-size changes. Never retain a target coordinate from a different layout.
- If the target disappears, dismiss or defer safely; never highlight an unrelated control.
- Do not queue a cascade of guides when several eligibility conditions become true.
- Preserve draft content and timer state across dismissals and replay.

## First-Launch and Today Step Contract

| Step | Target | Eligibility | Completion evidence |
|---|---|---|---|
| Orientation | Main navigation | Ordinary first launch, after Welcome | Explicit advance/skip |
| Create task | Today plus | Today visible, no editor or blocking operation | Editor actually opens, not pointer-down |
| Save task | Editor save action | User-owned valid draft | Repository save succeeds |
| Explore summary | Completed/Remaining card | Today idle | Corresponding sheet opens |
| Task actions | Real task row | At least one eligible row visible | Explicit advance or options opened; never require real completion |
| More | Header mascot | Main screen visible | More opens |

Empty Today must not wait indefinitely for a task row. Defer the gesture guide until one exists, and let users finish orientation without creating data. Keep the first-run sequence short; editor and gesture steps can occur later.

## Interaction and Accessibility

Do not steal existing long-press behavior from timer modes, task cards or the mascot. Keep action hit regions aligned with visible controls and provide accessible alternatives to gestures. Avoid requiring a timed gesture, color recognition or animation to understand an action. Respect reduced motion and focus restoration after dismissal.

Permissions, destructive actions, starting timers and recording outfits always require normal explicit user actions. Tutorial progression must not invoke these operations itself. A settings reset and a guide-progress reset must be separate operations.

## Verification Gates

- Fresh installation: Welcome followed by optional orientation, no immediate permission cascade.
- Existing installation: existing route/data preserved; no forced Welcome replay.
- Skip: no automatic return on next launch; replay remains possible.
- Process restart/rotation: no duplicate guide and no lost user draft.
- Save failure: guide does not claim success or silently advance.
- Deep link/widget launch: intended task or timer remains the destination.
- Empty Today, completed-only Today and offscreen rows: no missing-target crash.
- Large text, small viewport, keyboard, light/dark and reduced motion: no clipped targets or obscured essential actions.
- TalkBack: sensible focus order, accessible actions and restored focus on exit.
- Running/paused timer: guide navigation does not reset, pause or start it.
- Guide replay: no task, goal, outfit or permission mutation just from opening it.

## Handoff

Progress persistence is implemented in settings/GuideProgress.kt and GuideProgressPreferences.kt, exposed through SettingsRepository.observeGuideProgress and updateGuideProgress. Observe a GuideKey(id, version), then pass the observed GuideProgress as the expected value with a GuideAction. A false result means stale state: reload rather than blindly retrying. Storage errors propagate; consumers must not advance UI on a failed write. Revision checks reject callbacks from before a skip/replay, even if the visible step matches.

Native preference keys are namespaced by ID and version, so editing one guide preserves other guides, future keys and ordinary settings. Missing keys are unseen; malformed or unrecognized state suppresses automatic offers and can be explicitly replayed. Read errors propagate rather than pretending the user is new. In-progress state retains a stable step ID for resumption; allowsAutomaticOffer is not a full routing policy. The host must check visibility, interruptions and safe resumption separately. Guide definitions must validate that step IDs belong to their own version before calling the persistence API.

Unit tests cover transitions, invalid input, stale/concurrent callbacks, preference isolation, corrupted records, store reopen and failed writes. No Gradle, emulator or device operations were performed in this slice; tests are unexecuted. Guide state is local preferences and is not added to the app's explicit encrypted-backup format. First-launch/Today delivery and introduction replay are now connected. Next: manual acceptance, remaining guide flows and replay choices. Review pending content audits before treating the guides as exhaustive app help.
