# Editorial theme implementation — 2026-11-05

## Public beta typography update — 2026-11-17

The former OCR A, SF Pro, and Warping resources are not release assets. SF Pro
must not be bundled outside Apple platforms, and no redistribution record is
available for the other two binaries. The public beta uses OFL-licensed Funnel
Display (`res/font/funnel_display_variable.ttf`, SHA-256
`B4151C9C4B7B07EB74320096B4FF4156CCA8821F5ADFAB34AF9FD9A2D6C1179D`) for
display typography and retains OFL-licensed JetBrains Mono for readable body
text. Old saved typography enum values safely fall back to Funnel Display.
`assets/licenses/funnel_display_ofl.txt` travels with the font.

## Active visual authority — 2026-11-10

Editorial Field Notes is the authoritative DayLoop visual system. It supersedes
the older matcha/mint direction in `DAYLOOP_VISUAL_CONTRACT.md` for new work:
use neutral surfaces, black/charcoal text, orange `#FF6A24` only for purposeful
actions and rules, 24dp page margins, and compact 2–8dp geometry. The selected
app font applies to every Material typography role; the legacy readable-body
preference remains stored for compatibility but no longer overrides that choice.

### Edit-surface rule

Use a bottom sheet only for a focused, reversible choice or quick action with at
most two inputs. Open every multi-field create or edit flow full screen. This
includes Task Editor and Outfit Record, so their hierarchy, safe areas, and
primary action remain stable while the keyboard is open. A full-screen form
must use the page margin token and shared DayLoop action buttons.

## Follow-up — 2026-11-06

Fixed icon navigation disappearing: TooltipBox did not expose the Row weight as intended; each tab now has a direct weighted Box. Physical-device screenshots verified all three icons, and taps opened P&Gs and Focus. Added NavigationLayoutRegressionTest (not run).

Replaced the old wardrobe list with an adaptive photo grid, category filters, archive visibility, per-item actions and a photo-first creation flow using the existing crop/import/link repositories. Today now uses the wardrobe-door icon from the reference. Project cards use compact divided rows; tabs are underlined, Focus sound sits below the timer, task corners are rectangular, and the calendar no longer has an oversized mascot. Builds passed and updates installed with adb install -r; no data cleared. Photo creation and archive/delete end-to-end operations were not exercised against user data.

The user supplied a replacement font, Downloads/ocr-a bt.ttf. It replaces res/font/ocr_a.ttf while retaining enum OCR_A and preference keys; visible option is OCR A BT. SHA-256: 6DF6E9E833F112726F3C4594CB68845FA25959BA69BCD3E6FA333D17552B32C2. Existing typography preference remains untouched.

## Direction

Apply the accepted black/charcoal and orange reference using shared Compose tokens. Keep current task creation sheets, reminder/timer behavior, wardrobe storage and guide ownership. Do not replace unrelated work in this heavily modified checkout.

## Implemented

- Orange #FF6A24 fills with black foreground, neutral light/dark surfaces, smaller rectangular corners and 24dp page margins. Accent text has separate darker/lightened orange roles for light/dark surfaces.
- User-supplied OCR-A.ttf bundled as res/font/ocr_a.ttf. Default headings, labels and timer digits use OCR A; body text uses JetBrains Mono by default.
- Advanced > Typography provides OCR A / JetBrains Mono and a saved readable-body toggle. New DataStore keys editorial_font and readable_body_text deliberately give existing installations the new default; the legacy font key is preserved for rollback.
- Navigation uses calendar, layers (P&Gs), and timer silhouettes at 24dp, icon-only labels exposed through semantics/long-press tooltips, selected underlines, rectangular add action and clipped touch feedback.
- Today greeting and progress strip are compact; counts can wrap. Outfit prompt is outlined, task surfaces are neutral. Existing saved-outfit hiding/calendar review behavior is retained.
- Project cards have reduced padding/type size/height. Focus timer uses neutral surface and correct action-icon foreground. Wardrobe buttons/cards adopt smaller corners.
- Advanced opens from the right with dismissible scrim and safe-area/keyboard padding. Typography, guides and reminder controls expand on demand; recent-event clear/expand actions have labeled tooltip icons. Existing handlers and error messages are retained.

## Verification and continuation

Source/resource checks only. Font SHA-256 matches the user file: 8295F1FF9A595B3227403C47AEDEEF66B7B5D1AFF07EB010A8AA6E97B34075C7. New navigation vector parses as XML. Gradle, tests and emulator/device checks were not run.

Added EditorialTypographyPreferencesTest for defaults, persisted selection/readability and profile preservation across store reopening. Updated the existing page-margin token assertion.

Next verification: build; run those tests; manually inspect light/dark themes, 320dp width, large system text, keyboard-open settings, navigation tooltips, guide targets, timer actions and saved outfit review. This is a styling implementation, not a claim of pixel-perfect mockup conformance. Wardrobe retains its existing functional list rather than introducing an unverified gallery rewrite.

## Future clothes model

No custom-model settings or inactive ML switches were added. Preserve the staged plan: manual photo/description wardrobe storage first; later an evaluated clothes extraction/classification model with explicit consent, local/offline behavior where practical, confidence handling and user correction before saving. Existing extraction helpers are outside this styling change.
