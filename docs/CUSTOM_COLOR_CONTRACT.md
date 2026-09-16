# Matcha-relative custom color contract

The user's six-digit RGB HEX is a palette seed equivalent to #A8CF3D in the default matcha palette. It is not copied into every container.

## Derivation
- Always use the original light/dark matcha role tables, independent of the previously selected preset.
- Convert seed and role colors to CIE Lab using AndroidX ColorUtils. Preserve each role's hue offset and chroma ratio relative to matcha.
- Preserve the relative distance of each lightness value toward black or white around the anchor. Clamp the derived anchor lightness to 30-80 for extreme inputs so black/white seeds retain useful surface separation.
- Return matcha unchanged when the seed is #A8CF3D. Monochrome seeds generate neutral ramps.
- Correct text/foreground tones to reach 4.5:1 on their backgrounds. Main accents used as text are checked across surface levels. Outlines target 3:1 on the base surface. Consequently, the displayed primary may be a darker/lighter shade of the exact supplied seed; surfaceTint retains the seed and dark action surfaces get a separately readable accent.
- Secondary/tertiary roles, all surface-container levels, background, outlines, inverse roles and DayLoop's app-field/progress/action tokens are derived. Projects use secondaryContainer, goal badges use primaryContainer, and Advanced uses surfaceVariant.
- Error roles and warm warning indicators remain semantic. Calendar past-day colors retain the explicitly requested #002A32 / #E8E8E8 values. Supplied brand artwork is not recolored. Glance remains a separate theme.

## Input and fallback
Accept RRGGBB or #RRGGBB, normalize to uppercase, reject non-hex and alpha-bearing values. Invalid stored data falls back to the selected preset; invalid input cannot be applied. Emerald and Rose now use the same derivation model as custom HEX.

## Alternatives reviewed
A hue-only substitution was rejected: it leaves lightness/chroma differences uncontrolled for very dark, pale or saturated seeds. A matcha-relative Lab transform with contrast correction was selected because it preserves the existing role hierarchy and can be tested deterministically.

## Verification
Scoped Android tests cover black, white, gray, red, blue, green and rose seeds in both modes, semantic text/background contrast, distinct surfaces and unchanged error roles. The original anchor has an exact-identity regression. These checks do not retroactively certify the default matcha palette's existing accent-text contrast.

Reference: [AndroidX ColorUtils](https://developer.android.com/reference/androidx/core/graphics/ColorUtils).
