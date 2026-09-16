# Supplied Asset Import

2026-09-08. Sources: `icon_files/`. All originals preserved; supplied `mascot_animation.xml` was not read or imported.

## Applied

| Source | Android use |
| --- | --- |
| launcher-background.svg | Full-bleed adaptive background on 108 dp canvas |
| launcher-foreground.svg | Centered 56 dp wide foreground on 108 dp canvas |
| monochrome.svg | Same padded layout, Android 13+ themed launcher |
| notification-small-icon.svg | Centered 17 dp wide silhouette on 24 dp canvas, both notification builders |
| no-bck-splash-light.svg | Android 12+ light launch: white mark on brand green |
| no-bck-splash-dark.svg | Android 12+ night launch: green mark on near-black |
| coolicons: Calendar / Calendar_Check | Today tab |
| coolicons: Calendar / Calendar_Days | Plan tab |
| coolicons: Calendar / Timer | Focus tab |
| coolicons: Navigation / Flag | Goals tab |
| coolicons: Menu / More_Horizontal | More tab |

Splash marks are 160 dp wide on 288 dp canvases. Android 8-11 retain the existing window launch appearance. Launch resources follow system night mode; the saved app theme applies after settings load. No splash Activity or timer was added.

Navigation groups retain their original 24 x 24 design grid. Compose supplies theme tint and preserves the existing state-driven icon slot for future animation. Supplied emerald branding colors were preserved; the matcha app palette was not replaced.

## Ready, Not Displayed

`mascot.svg`, `mascot-extra.svg`, `wordmark-light.svg` and `wordmark-dark.svg` are converted to `drawable/dayloop_mascot`, `dayloop_mascot_extra`, `dayloop_wordmark_light` and `dayloop_wordmark_dark`. Choose wordmark variants after inspecting contrast on the welcome surface, not solely by filename. First-run preferences, welcome routing and replay behavior were not added.

## Left as Supplied

- Square splash variants: alternatives not used by the native splash.
- Play Store SVG: still needs an opaque 512 x 512 PNG export, not an app drawable.
- Supplied animation XML: explicitly excluded.
- ZIP marketing/category backgrounds and unrelated icons: not imported.

## Reimport

`scripts/import-brand-assets.ps1` runs `scripts/ImportBrandAssets.java` with cached Android sdk-common 31.13.2. It converts assets only; no downloads or Gradle. A compatible JDK and cached dependencies are required. Generated drawables are overwritten; manual manifest, theme and Compose wiring remain separate. Converter diagnostics stop processing, but earlier outputs may already exist.

## Attribution

coolicons by Kryston Schwarze, [original source](https://github.com/krystonschwarze/coolicons), [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). Changes: selected groups extracted, translated to local viewports, converted to Android vectors and tinted. No endorsement implied. Attribution/source/license links also appear in More > Icon credits.

## Status

Fifteen conversion outputs completed without converter diagnostics. This is asset processing, not app compilation or visual verification. No Gradle, lint, tests, emulator, installation or device checks ran. Manual review remains for launcher masks/themed icon, splash contrast, notification size and all tabs in both themes.
