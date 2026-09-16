# Onboarding and Asset Handoff

Prepared 2026-09-08. Onboarding is a recommendation awaiting the user's artwork and acceptance; it has not been implemented in this batch.
Update: artwork supplied in `icon_files/` was imported on 2026-09-08. Launcher/notification/navigation/splash are connected in source. Mascots and wordmarks are ready; onboarding behavior is not implemented. See `ASSET_IMPORT.md`. App verification remains waived.

## Welcome screen

Use one first-run welcome screen, separate from Android's automatic launch splash.

- Pale mint surface, dark-mode equivalent, centered DayLoop wordmark, and supplied mascot/brand illustration.
- Three compact benefit rows: Plan your day with time; Focus on what matters; Build lasting momentum.
- One primary black/lime action: Start the day. Persist the first-run completion flag in DataStore, then open Today.
- No bottom tab bar, account requirement, name entry, or permission dialog on the welcome screen.
- Omit sign-in until account functionality actually exists. Request notification or exact-alarm access in the relevant reminder workflow.
- Returning users land on Today. Add a replay entry in More when onboarding is implemented.
- Do not turn the launch splash into a second interactive welcome screen.

## Assets to provide

SVG is preferred for simple artwork; the dimensions below are artboard/viewBox sizes for vectors. Raster equivalents are source exports, not sizes to display on screen. Brand/mascot dimensions are DayLoop recommendations, not Android platform requirements.

| Asset | Preferred source | Artwork requirements |
| --- | --- | --- |
| Adaptive launcher foreground | SVG, 108 x 108 viewBox; optional PNG 1080 x 1080 | Transparent outside the mark. Keep essential artwork inside the central 66 x 66 safe region, preferably within its inscribed circle. |
| Adaptive launcher background | Solid color hex or SVG, 108 x 108; optional PNG 1080 x 1080 | Fully opaque, full bleed. Separate from foreground. No baked-in rounded mask or drop shadow. |
| Monochrome launcher layer | SVG, 108 x 108 | Single-color silhouette on transparency, same positioning and safe region as foreground. Android supplies the themed tint. |
| Google Play listing icon | PNG, exactly 512 x 512 pixels | Full square, opaque artwork/background; maximum 1024 KB. Do not pre-round corners or add the external platform shadow. |
| Splash mark, no icon background | SVG, 288 x 288; optional PNG 1152 x 1152 | Essential mark within a centered 192-diameter circle (768 pixels on the 4x export). Reuse the brand mark rather than a phone-sized splash image. |
| Splash mark, with icon background (alternative) | SVG, 240 x 240; optional PNG 960 x 960 | Essential mark within a centered 160-diameter circle (640 pixels on the 4x export). Supply only the selected variant. |
| DayLoop wordmark | SVG around 1024 x 256; PNG fallback same size | Transparent, preserve its natural proportions. Provide versions for light and dark surfaces, or a single tintable mark. Intended display roughly 160-200 dp wide. |
| Welcome mascot/illustration | SVG or transparent PNG, 1024 x 1024 | Keep artwork separated from phone/background/text. Intended display around 200-280 dp. Provide a dark-surface variant only if needed. |
| Navigation and interface icons | SVG, 24 x 24 viewBox; PNG fallback 96 x 96 | Consistent stroke weight, optical centering and padding. We give each control at least a 48 dp touch target. |
| Notification small icon | SVG, 24 x 24; PNG fallback 96 x 96 | White silhouette with transparent background; no wordmark, colored square or gradients. |
| Goal badges / small illustrations | SVG or transparent PNG, 512 x 512 | Intended display 48-72 dp. Avoid embedded text that becomes unreadable at this size. |

The adaptive launcher has a 108 dp canvas and central 66 dp safe region: [Android adaptive icon guidance](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive).

Splash canvases and circle limits: [Android splash guidance](https://developer.android.com/develop/ui/views/launch/splash-screen).

Play listing requirements: [Google Play icon specification](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).

Icon importing and density outputs: [Android Image Asset Studio](https://developer.android.com/studio/write/create-app-icons).

## Naming and animation readiness

Suggested groups: launcher/, branding/, onboarding/, navigation/, actions/, badges/. The user can supply any directories; importing will map them into Android resources later.

- Launcher: foreground.svg, background.svg (or a color value), monochrome.svg, play_store_512.png.
- Branding: wordmark_light.svg, wordmark_dark.svg, splash_mark.svg.
- Onboarding: welcome_mascot.svg or welcome_mascot.png.
- Navigation: today.svg, plan.svg, focus.svg, goals.svg, more.svg.
- Actions: add, back, calendar, menu, check, play, pause, reset, delete, save, reminder, and arrow_right.
- Optional navigation active/inactive variants must share the exact viewBox, alignment and canvas. Keep labels and the selected capsule separate so future motion can animate them independently.
- Export SVG text as paths. Prefer ordinary paths/fills; avoid embedded fonts, linked images, scripts and unsupported filter effects.
- Do not flatten the icon into a screenshot or include the phone frame. Keep transparent assets transparent.
- No need to export every Android density yourself; source assets will be converted during a later explicitly approved import step.

## Current implementation boundaries

Focus and Idle now have source implementations. Onboarding, final assets, animated navigation, custom palettes, a standalone draggable schedule ruler, and project-protocol editing remain separate work. Idle is entered manually; neither Idle auto-start nor background timer notifications were added.
