# Widget refresh handoff

## Implemented source
- Today uses responsive compact (170x180dp) and agenda (280x320dp) layouts. Counts and
  first two tasks derive from the same Today snapshot builder and project generator as
  the app. Overdue, Now, Next and Anytime labels remain accurate. No planned work is
  distinguished from completed work. Titles obey widget privacy, including a fresh
  privacy check before completion. Add opens the editor; task rows open their task.
- All three widgets follow app light/dark/system mode, reference neutrals and lime.
  Glance uses native text, not rasterized Warping text. Emoji mood decorations removed.
- Goal uses a real proportional LinearProgressIndicator. It still chooses the first
  active goal and opens P&Gs, not a selected goal detail. Underlying goal weighting is unchanged.
- New Focus provider displays the existing session mode, finish time while running,
  remaining duration while paused, and today's focused minutes. Explicit start/pause/
  resume activity requests open Focus before invoking existing store/service behavior.
  No separate timer, background launch trampoline or per-second polling was added.

## State and navigation
provideGlance loads initial persisted data; active compositions collect the underlying
flows to avoid stale captured snapshots during a Glance session. Existing widget
reconciler now observes projects too and refreshes Focus. Widget providers request a
30-minute system refresh as a fallback; this is not an exact freshness guarantee.
Time-only transitions and midnight may lag until the next update when the process is
not active. Running Focus intentionally shows an end time rather than a frozen countdown.
MainActivity accepts widget_route separately from edit_task_id, consumes route requests
before execution, and DayLoopApp allowlists destinations/commands. Existing timer
mutations remain inside the app's busy/error handler. No notification permission bypass.

## Verification
Source checks only. No Gradle, emulator, physical-device, install or uninstall operations.
Two mapper regression cases added; all tests remain unrun. XML parsed successfully in
source verification. Launcher rendering and cold/warm action behavior are unverified.

Manual review: add/resize Today, check long titles and large text; compare private/public
titles; complete stored/generated tasks; verify Add and task links cold/warm; test Focus
start/pause/resume against the in-app timer; check Goal 0/partial/100% and light/dark.
Minimum widget footprints were enlarged to leave room for readable controls.

## Deferred
Configurable P&G selection, direct goal detail, Calendar widget, exact clock-boundary
refresh, native ticking countdown, richer previews, per-widget theme/privacy overrides,
custom-font parity and earlier-Android rounded-corner fallback. Calendar rhythm remains
blocked on reliable task completion history. Do not claim these features implemented.

## API references
- https://developer.android.com/develop/ui/compose/glance/build-ui
- https://developer.android.com/reference/kotlin/androidx/glance/appwidget/LinearProgressIndicator.composable
Glance cornerRadius(Dp) is available in the installed 1.2.0, with rounding on Android 12+.
