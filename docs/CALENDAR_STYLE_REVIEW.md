# Independent solutions and review

Scope: calendar-reference conformance, native font selection, decorative planning icons and Focus settings cleanup. Preserve current swipe host, timer transitions and advanced preferences.

## Candidate A: standard Compose, constraint-derived geometry
Header is a Column: large day, a baseline-aligned month/abbreviated-weekday row, then year. Grid is a Column of seven-cell Rows. One pure function computes circle diameter from both available width and height, including every gap and label band. Month padding is outlined circles, today accented, past days theme-specific, future days muted. Portrait stacks header/grid; landscape places them side by side. Actual text and semantics remain native.
Fonts use bundled resources selected through theme typography. Decorative icon preferences are keyed by entity type and stable ID in DataStore; Room remains the work source of truth. Existing reminder repair callback moves into the advanced section.

## Candidate B: custom measured calendar surface
A custom Layout measures the complete header and every date slot, placing them at reference-relative coordinates. A Canvas renders the grid from those coordinates. A parallel semantics tree labels date slots; text is measured with TextMeasurer. Fonts and decorative icons use the same resource catalog but icon fields could instead be added to Room with a schema migration.
Benefits: precise reference placement, few drawing nodes, direct coordinate control. Risks: manual semantics/hit regions, bespoke font-scale and RTL handling, more screenshot fixtures, migration cost for purely decorative choices.

## Reviewer comparison
Engineering estimates, not benchmark results. Scores are out of 5; higher is better.

| Criterion | A | B |
|---|---:|---:|
| Performance for <=42 circles | 5 | 5 |
| Small screen / six-week / font-scale correctness | 5 | 3 |
| Accessibility and native font behavior | 5 | 3 |
| Maintainability and local-pattern fit | 5 | 3 |
| Exact coordinate control | 4 | 5 |
| Total | 24 | 19 |

Verdict: select A, incorporating B's explicit geometry budgeting. No network font dependency at runtime; unknown preferences fall back safely. Do not fake task completion: past-day fill is chronological, while completion metadata only describes dates with completed scheduled work.

## Verification plan
Unit-test circle bounds for 4/5/6 rows and narrow/short hosts. Compile APK and run only this slice's tests. Use a focused instrumentation render test for portrait, landscape, six-week month, both themes and enlarged text, capturing images for inspection. Report exact commands/results and any blocked device checks; do not infer conformance from a successful build.

## Iterative findings
- Build compiled production code; stale diagnostics test doubles and an appearance fixture blocked test compilation. Updated only missing clear-history methods/callbacks; unrelated suites were not executed.
- Calendar geometry JVM tests passed. Windows DataStore file replacement failed in the new persistence fixture; moved this platform check to Android, where the font/icon persistence test passed.
- Initial Android render tests found 2-4 pixel distortion in the last circle of rows at emulator density. The dp formula was mathematically valid but independently rounded child widths overran the parent. Final geometry budgets integer pixel gaps/diameters first, then converts to dp.
- Review also removed direct repository reads from PlansScreen and propagated the selected font into the newer fullscreen Focus timer. Existing timer auto-transitions were preserved.
- Final pixel-aligned render verification is in progress; do not treat earlier successful APK output as visual acceptance.
