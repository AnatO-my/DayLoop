# DayLoop Editorial Field Notes visual contract

This is the active visual contract for all new DayLoop UI work. It supersedes
the archived matcha/mint guidance in `DAYLOOP_VISUAL_CONTRACT.md`.

## System

- Use the semantic roles in `MaterialTheme.dayLoopTokens` for application
  surfaces, text, borders, status colours, and actions. Reserve
  `MaterialTheme.colorScheme` for Material components' own defaults.
- Light and dark themes use neutral paper/charcoal surfaces. Orange `#FF6A24`
  is reserved for meaningful action, selection, rules, and emphasis.
- Use `spacing.pageMargin` for horizontal screen content, then use the other
  spacing roles rather than duplicating dp literals.
- Geometry is compact: `smallCorner` 2dp, `inputCorner`/`cardCorner` 4dp,
  `sheetTopCorner` 6dp, and `dockCorner` 8dp. Do not introduce 12–28dp
  rounded cards or pill buttons.

## Typography and copy

- Every Material typography role uses the selected app font. Do not create a
  body-only font override.
- Define and use all Material text roles through `DayLoopTypography`.
- Put user-visible text, dynamic accessibility labels, errors, and dialog copy
  in string resources. Keep model identifiers and persisted enum values out of
  resources.

## Components and surfaces

- Use `DayLoopButton` and `DayLoopOutlinedButton` for primary and secondary
  application actions. Use `DayLoopIconButton` only when the action is clear
  from its accessible label.
- Screen-owned cards, rows, and overlays read their colours and shapes from
  DayLoop tokens. Material controls may retain their Material defaults.
- Bottom sheets are only for focused, reversible choices with no more than two
  inputs. Multi-field creation and editing flows are full screen.

## Compatibility

The old palette preference remains persisted solely so existing installations
can upgrade safely. It is not exposed as a setting and cannot alter the fixed
Editorial Field Notes theme.
