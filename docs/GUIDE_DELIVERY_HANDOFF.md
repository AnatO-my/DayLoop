# Guide Delivery Handoff

Date: 2026-09-21
Status: Implemented; latest changes not build- or device-verified.

## Existing Work Preserved

Continued the GuideDefinitions, GuideUi, TodayRoute and component-target implementation already present in the working tree. Existing guide content and unrelated wardrobe/media changes were retained. The separate STATE entry reporting a successful Kotlin build describes the preceding implementation, not verification of this hardening patch.

## Changes

- GuideDefinitions.nextAction resolves the following step rather than advancing to the same ID; the last step completes.
- Selection waits for stored progress, avoids first-launch orientation for existing users and permits interrupted orientation to resume.
- TodayRoute catches storage failures, guards writes and presents only persisted IN_PROGRESS steps. Failed writes do not advance guidance.
- DayLoopApp tracks first-launch eligibility, external entry and session completion. One skip/completion prevents an immediate cascade into another guide.
- Foreground, route, task loading, app mutation, settings, notes, task actions, errors and running focus timers gate delivery. Widget/notification entry suppresses automatic guidance for the session after consumption too.
- Wardrobe/outfit dialogs suppress delivery; the metric sheet hides it while open.
- Targets use observable host-relative rectangles and disposal cleanup. The mascot alone is targeted; only the first task across visible sections registers the task anchor.
- Missing targets retain Next/Skip without stale highlights. Target modifiers outside a host are harmless.
- The measured panel height is bounded and scrollable. Drawing does not intercept the real screen controls. Back skips when not saving.

## Limits

- This is orientation, not evidence of a successfully created task. Next acknowledges a step. Event-verified editor/save guidance remains pending.
- Targets are not automatically scrolled into view. Missing lazy-list targets permit scrolling, advancing or skipping. Offscreen task-gesture steps are not individually queued for later teaching yet.
- Introduction replay is available in More; selection/replay of other guides remains pending. Check current source for additional guide wiring rather than treating this historical slice as exhaustive coverage.
- Large-text placement needs device inspection, particularly when the target and panel cannot both fit without overlap.
- Guide progression itself never creates tasks, grants permissions, starts timers or records outfits.

## Verification

Authored GuideDeliveryTest and GuideHostTest. Not executed. Source call-site review completed; scoped whitespace check still reports existing unrelated Today whitespace. No build, emulator or device operations were performed for this patch.

Manual checklist:
1. Fresh state: complete Welcome; Next progresses and Skip exits without creating data.
2. Existing state: no forced Welcome; Today guide only on eligible normal entry.
3. Skip/complete: no immediate second guide; restart confirms persistence.
4. Widget/notification entry: intended destination stays unobstructed.
5. Open More, metric details, wardrobe, outfit dialog or task editor: guide does not obstruct the modal.
6. Empty Today/offscreen task: no stuck overlay or stale highlight.
7. Rotate, scroll, enlarge text and change theme: inspect placement and readable controls.
8. TalkBack/Back: check focus order, announcements and dismissal.
9. Storage failure: no false completion; dismissing the error does not reopen guidance repeatedly.

Update: introduction replay now exists in More's header. Next: manual acceptance, other replay choices and remaining contextual flows. Do not mark the entire guide project complete.
