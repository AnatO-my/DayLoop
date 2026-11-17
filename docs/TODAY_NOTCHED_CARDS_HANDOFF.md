# Today notched metrics: implementation handoff

## Scope and status
Implemented source for the two Today summary cards only. Preserve the current greeting,
typography, theme, navigation and task editor. No build, tests, emulator or physical-device
operations were run. Manual visual acceptance and compilation remain pending.

## Geometry and interaction
TodayMetricCard.kt owns TodayMetricShape: a rounded outline with a concave top-right
notch constructed from quadratic curves. The 48dp arrow disc fits within the 56dp notch,
leaving an 8dp gap. The shape is not a painted background patch, so it works on either
theme. Card content starts below the notch to keep larger text and counts clear.
The existing forward drawable is rotated -45 degrees; no external asset is required.

The parent clips its ripple to a combined outline containing the card and arrow disc.
It has exactly one clickable/accessible action. The disc is decorative within that action,
not a nested button. Both the disc and card open the same sheet. The layout's hit box
remains rectangular (Compose clipping is a drawing boundary, not arbitrary-path hit
testing); the transparent gap may therefore also activate the same harmless detail action.
No touch-target shrinking or global ripple suppression was introduced.

## Data and navigation
TodayScreen stores only the selected sheet key with rememberSaveable. Completed reads
completedSection; Remaining reads visibleSections, including Overdue, Now, Next and
Anytime. Counts continue to come from the existing snapshot; no task state is duplicated.
Sheets recompose from the current TodayUiState. Opening a row dismisses the sheet before
calling the existing onEditTask callback. Disabled actions disable cards and sheet rows.
Empty sheets have explicit messages; Close, outside dismissal and Back use the modal API.

The former inline scheduled-only expansion was removed. The lime card now says Remaining
because its value already uses remainingCount, not total planned tasks. Its date uses the
snapshot date rather than a separately remembered wall-clock date. Project titles were
added to TodayRoute's remember keys so renamed projects refresh within these sheets.

## Deliberate limits
No completion timestamp or undo button was invented: the current TodayTaskUiState does
not expose a completion timestamp, and TodayScreen has no undo callback. Add those only
through the existing domain/repository path in a separately approved slice. Do not change
completion semantics or assume an occurrence can be undone by toggling a Boolean.

## Resume checklist
1. Read AGENTS.md and STATE.md, then these three feature/today source files.
2. Compile and run TodayMetricTest only when verification is authorized; two UI tests
   were authored for empty sheet opening/dismissal and disabled actions, not executed.
3. Review light/dark, narrow width and large text: curves, disc gap, equal metric heights,
   no count/date clipping, one TalkBack action per card and bounded pressed feedback.
4. Review populated Completed and Remaining sheets, especially overdue/anytime tasks,
   long titles/project names, live task changes and task-editor navigation.
5. Review rotation/sheet restoration and dismissal. Do not run connected tests, install,
   uninstall or clear app data on a physical phone without explicit authorization.
