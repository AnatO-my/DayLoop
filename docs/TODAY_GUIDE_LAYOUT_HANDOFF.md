# Today and Guide Layout Repair

## Cause
DayLoopApp wraps routed content in GuideHost, and Today, Plans, Focus and TaskEditor each also wrap their content in GuideHost. Both layers received the same step and drew the same panel. Each owned a separate CompositionLocal target map, so the outer panel also lacked inner screen anchors. This explains duplicated instructions at different vertical positions; it was not duplicate database data.

GuideUi now lets nested hosts render their content directly under the existing target registry. Only the outer host draws the overlay and handles actions/Back. Standalone previews/tests still get their own host. The app-level controller remains responsible for guide eligibility/suppression; nested activeStep/busy/action arguments do not override the outer controller. Future modal-gating changes belong there. No guide progress is reset or deleted.

## Today
- Header moved from a lazy-list item into a fixed Scaffold topBar with statusBarsPadding. Content retains measured top clearance and the existing bottom dock scroll clearance.
- Greeting reduced from displayMedium to headlineMedium; no viewport-based font scaling.
- Outfit prompt waits for initial outfit data and hides after the database supplies today's record, including photo-only records. No extra persisted hidden flag. Clearing the record brings the prompt back; suggestion previews reset when date changes.
- A small Wardrobe shortcut remains after saving, because removing the full card must not remove management access. Saved outfit review/edit remains available by opening the idle calendar and tapping its day.
- Removed the outfit card's TODAY_SUMMARY anchor, which collided with the metrics. Outfit availability is passed from the route instead of opening another wardrobe subscription in the card. Empty wardrobe drops disabled Suggest/Record controls; Import uses the existing restore icon/tooltip and Wardrobe stays single-line.

## Verification
Authored GuideHostTest nested-host regression (single title/message/buttons and outer action only), plus TodayLayoutRegressionTest for fixed header, loading, photo-only saved records, clearing and suggestion retention until persistence. Corrected old contradictory missing-target assertion: the fallback guide panel remains available when the target disappears.

No tests, build, emulator or device checks were run. Source/target/call-site review only. Older TodayScreenTest assertions about Score/Streak/checkboxes are already stale and require their own cleanup.

## Manual Check
Replay guides on Today/P&Gs/Focus and editor: one panel, correct highlighted control, one action per tap. Scroll Today and confirm its title never crosses behind status icons. Repeat in light/dark and large-font settings. Save an outfit, reopen Today, confirm full prompt disappears and Wardrobe remains reachable. Open calendar and edit/clear today's record; clearing should bring the prompt back. Check photo-only records and a new day. No user records should be deleted by any layout change.
