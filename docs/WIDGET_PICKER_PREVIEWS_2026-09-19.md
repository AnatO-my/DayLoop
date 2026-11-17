# Widget Picker Previews

## Scope and status
User approved distinct widget names and faithful picker previews. This slice changes presentation only: no new widget provider identities, grid footprints, task mutations or timer semantics. Existing unrelated edits remain intact.

Implemented in source; resource checks and static image inspection passed. No Gradle, lint, Kotlin tests, emulator, ADB, install/uninstall or physical-device commands ran. Runtime compilation and launcher acceptance remain unverified.

## Architecture
- Manifest receiver labels already existed. Their string values now read DayLoop Today, DayLoop Goals, DayLoop Focus. Focus description no longer promises an undisplayed streak.
- All three provider XMLs reference dedicated `dayloop_*_picker` images and preview layouts. The prior user-provided Today PNG remains untouched but is no longer selected by metadata.
- Six PNGs in drawable-xxxhdpi/drawable-night-xxxhdpi use transparent rounded corners, reference widget colors and compact minimum dimensions at 4x density. The fallback represents compact Today, not agenda Today.
- `scripts/render-widget-previews.ps1` regenerates these source-layout illustrations using Windows System.Drawing. Arial approximates native sans typography; these are not Android screenshots. Keep sample content, spacing and colors aligned with live Content and WidgetPreviewSamples when changing UI.
- Each widget extracts its existing Content function and invokes it from both provideGlance and providePreview. Live collectors, privacy rules and actions are unchanged. previewSizeMode uses the same responsive sizes, including Today's expanded agenda. Preview controls are decorative: no PendingIntent/completion callback is created for sample data.
- WidgetPreviewSamples contains fictional task IDs/titles, 3 remaining/2 completed, Read 10 books at 40%, and a 25:00 Pomodoro with 45 minutes. Preview code reads only theme preference, never private tasks/goals/timer state.
- MainActivity's theme/configuration effect calls WidgetPreviews on Android 15+. Publisher serializes requests, checks provider generatedPreviewCategories and persisted revision/theme stamps, then uses Glance 1.2 setWidgetPreviews. Increase REVISION when layout/sample content changes.
- A successful publication is cached per provider. Rate-limit/error attempts are not retried in a loop; retry is eligible on another process launch. Cancellation propagates and clears the attempt marker. Static resources stay available if generation fails. No background polling or service is added.

## Compatibility limits
- Generated previews follow the app theme at publication. Static resource qualifiers follow the launcher's resource configuration, so a forced app theme may differ on older/OEM pickers.
- HyperOS decides whether it uses generated previews, layout previews or PNGs, and can cache older labels/artwork. Its outer picker UI and displayed grid-size label remain launcher-owned.
- One resizable Today provider stays one picker entry. No separate compact/agenda widget added.
- MainActivity must open once for publication. Before then the packaged PNG/layout is available. Successful source checks do not establish OEM behavior.

## Checks and manual next step
Executed `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-widget-previews.ps1`: PASS for distinct labels, provider/layout image references, six dimensions, transparent corners, background colors and lime controls. Inspected representative Today/Goal/Focus PNGs; no clipping observed.

1. User builds and updates the app without uninstalling or clearing app/launcher data.
2. Open DayLoop once, then close/reopen the widget picker.
3. Check three names and sample layouts instead of the app icon, in light and dark modes.
4. Add/resize Today, Goal and Focus; confirm their existing actions still work and generated Today agenda remains readable.
5. Check a fresh install before first launch/older Android for PNG fallback. If HyperOS retains an old preview, reopen/restart the launcher normally; do not clear launcher storage as that can erase the home-screen arrangement.
6. If rate-limited during repeated theme changes, retry by launching the app later after the platform quota resets. Do not add a busy retry loop.

Official API guidance: https://developer.android.com/develop/ui/compose/glance/generated-previews
Receiver label guidance: https://developer.android.com/develop/ui/compose/glance/enhance
