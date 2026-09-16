# UI Revision: Manual Review

2026-09-08. Source implementation only. No Gradle, build, lint, test, install or emulator commands were run for this batch. Earlier passing results do not validate it.

## Latest requested changes
### Focus duration follow-up
- Long-press each mode label. Save an integer from 1-180 minutes; invalid/empty values must disable Save. Cancel leaves preferences unchanged. Use default changes the draft until Save.
- Verify all three durations remain independent and persist after restart. An untouched selected timer updates immediately.
- During running and paused sessions, edit the current mode duration: deadline/remaining time stay unchanged. Reset uses the new duration. Starting after completion uses the new duration.
- Complete a custom Pomodoro and verify the credited minutes match that session, including when its default was edited mid-session. Breaks add no focus minutes. Repeated completion processing must not double-credit.
- Tapping the already-selected mode no longer resets its timer; the Reset button remains available. Check accessible long-click actions and large-text labels.
- Three FocusDurationTest instrumentation cases authored but not executed. No build, emulator, alarm or background timer changes.

### Projects/Goals follow-up
- Tap still edits. Long-press either card opens Archive (or Reactivate for archived records) and Delete; TalkBack exposes the long-click label.
- Archive removes the record from the default list. Show archived includes it again with an Archived status; Reactivate sets ACTIVE. Links and saved child work remain.
- Delete requires a second confirmation naming the item. Cancel must leave everything unchanged. Deleting a project retains saved tasks/reminders but clears their project link; deleting a goal retains projects/tasks but clears the projects' goal link. Unrelated records must remain unchanged.
- Archiving/deleting a project stops its new protocol-generated projections. Already saved task records are retained. Archiving a goal does not archive its projects.
- Check immediate list updates, repeated taps while saving, persistence after restart, and long titles/large text in the action dialog. Two new PlanningDeletionTest Room tests are authored but unrun; builds/device checks remain waived.

### Idle-only follow-up
- Open from Today, Plan, Goals, Focus, More or task editor by dragging down at least 64dp starting within the app's top 88dp below the status bar. The OS status-bar gesture is not intercepted. Welcome and existing modal dialogs are not entry surfaces.
- Swipe up at least 64dp anywhere on the cover, tap the exposed strip below, or use Back to dismiss. Verify the previous tab, list position and unsaved task draft remain intact.
- Verify short drags and horizontal gestures do not open it; normal list scrolling below the top region and task swipe actions still work.
- Check months with four, five and six weeks, portrait/landscape, and large text. Past dates must be #002A32 in light mode and #E8E8E8 in dark mode, independent of completion; today uses the selected palette accent.
- Check TalkBack Open idle calendar and Dismiss actions. Today calendar and More idle buttons are removed; welcome replay and other More behavior are deliberately unchanged.
- This cover has no background auto-start, alarms or extra polling. Source reviewed only; all device checks remain pending.

### Previous revision
1. Today: equal-height centered metric cards with reserved label space; compact typography. New tasks default to an explicit planning date of today. Successful root writes refresh classification time; repository observations supply task changes.
2. Add controls: centered accessible plus-only buttons on Today, Projects and Goals.
3. Planning: removed the duplicate Projects/Goals switch, not the stored relationships. Separate destinations retain project editing, goal editing and derived progress.
4. Navigation: one inset-aware four-tab dock across main screens. More is opened by the top-left menu. Active icons are white with no coloured selection bubble; reduced-motion disables the selection bounce.
5. Typography: supplied JetBrains Mono static fonts bundled under res/font, with its OFL license under assets/licenses. The separate Glance widget is unchanged.
6. Task editor: compact Cancel/title/Save header, filled title field, schedule and reminder menus, progressive schedule options and secondary actions. Unsupported description, members and estimate persistence were not invented from the inspiration image.
7. Pickers: Material calendar and clock/keyboard dialogs for task deadlines, planning date, recurrence bounds, block/interval times, project cycle anchor and goal target. Numeric durations remain numeric inputs. These are native Compose controls, not a custom reproduction of the spiral inspiration.

## Earlier approved implementation included
- First-run welcome and replay from More, using supplied wordmarks and mascot; no sign-in or permission prompt introduced.
- Persisted palette, text scaling, reduced-motion and existing theme mode.
- Left swipe completes; right swipe opens snooze/reschedule; two-second hold opens other actions. Accessibility actions remain available. Snooze respects reminder policy and active occurrence state.
- Project active/rest cycle editor and version-aware updates.
- Staged schedule editor and draggable time ruler. Saving the editor changes the draft, not platform alarms directly.
- Supplied coolicons action assets imported; original files and mascot_animation.xml untouched.

## Manual checks
- At normal and enlarged text sizes, inspect Today metrics, Focus mode labels, header, navigation and keyboard spacing in both themes and each palette.
- Save a new default task and see it immediately under Anytime. Edit its title/date, complete it, reopen it and check counts. Future-dated work should not be forced into Today.
- Check deadline, block, interval and recurring work separately; the explicit planning-date fix does not change the existing interval/recurrence Today-classification rules.
- Visit all four tabs and More. Check dock position and that plus controls do not overlap content. Verify Back behavior and preserved drafts.
- Open every date/time control, confirm and cancel; check cross-day blocks, optional clear, invalid ranges, and 12/24-hour settings. Check small devices and large system fonts.
- Verify gestures do not conflict with vertical scrolling; rejected writes must not silently remove rows. Check TalkBack alternatives and confirmation before deletion.
- Check welcome first run/replay, saved appearance after restart, project cycle edits and ruler cancel/save.

## Still outside scope
Background idle auto-start, focus alarms/notifications, widget parity and Play Store artwork export remain separate. Existing automated UI tests may require updates for the new form/navigation. Newly authored unit regressions have not been executed.
