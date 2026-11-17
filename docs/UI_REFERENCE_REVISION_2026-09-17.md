# Reference UI revision - 17 September 2026

## Scope
User-requested Today, Focus summary, P&Gs, editor selection, appearance and bottom navigation changes. Existing unrelated edits, persistence, timer service, sounds, mascot and archive/delete behavior preserved.

## Changes
- Today metrics use completedCount and remainingCount from the existing Today snapshot. Violet/lime fills, equal row heights, stacked below 320dp content width or above 1.25 font scale.
- Focus retains actual recorded session minutes and displays them as Focused time today.
- Theme bypasses custom seed/palette and selected font; JetBrains Mono fixed. Saved preferences retained for backup compatibility, not silently deleted. System/Light/Dark remains available.
- Palette presets, HEX input, saved palettes and font selector removed from AppearanceControls. Font weight, assistant and reduced-motion switches retained.
- Task cards use inverseSurface/inverseOnSurface: white on dark canvas, near-black on light canvas. Title, timing badge, project and description have separate rows.
- P&Gs label replaces Plans navigation label and oversized side-by-side header. Projects/Goals remain internal tabs with unchanged record semantics.
- Project cards fill available width and use content-driven height instead of fixed 232dp squares.
- Four navigation slots remain in the pill regardless of route. Plus is a separate centered control above the dock, only on Today/plan routes. Explicit dark-on-lime icon color.
- Schedule and reminder options use two-column selectable grids with wrapping labels. Project selection uses a full-width field and scrollable full-name dialog.

## Verification
Source inspection only. Three ReferenceUiTest cases authored for fixed palette/font, route-dependent add action and schedule selection. Not compiled or executed. No emulator, phone, install, uninstall or build commands.
Existing DayLoopAppearanceTest contains stale token references from an earlier UI generation; broader instrumentation compilation may require a separately scoped fixture update.

## Manual acceptance
1. With previously saved red/custom palette and alternate font, reopen: reference colors and JetBrains Mono must apply. Switch Light/Dark/System.
2. Compare completed and remaining counts with today's records; change completion and re-open Today.
3. Check violet/lime metric alignment at normal and enlarged text. Check task foreground/background contrast in both modes.
4. Open P&Gs; switch Projects/Goals. Inspect long titles, archive controls and full-width progress cards.
5. Switch Today/P&Gs/Focus repeatedly: dock tabs do not redistribute; plus appears only above Today/P&Gs and opens the correct editor.
6. In New task, select each schedule option, cancel/confirm block or interval editing, select a long project name and change reminder mode.
7. Confirm Focus daily minutes still match recorded sessions; timer/notification behavior is outside this patch.
