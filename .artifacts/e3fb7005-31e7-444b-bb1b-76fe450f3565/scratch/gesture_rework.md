# Implementation 1: Unified Navigation Host

This approach uses a single `Modifier.pointerInput` at the root of the app shell to manage all gesture-based navigation.

- **Architecture**: Create a `DayLoopNavigationHost` component that wraps the main scaffold.
- **Top Gesture**: Detects downward swipes in the top 88dp. Triggers `IdleCalendar`.
- **Bottom Gesture**: 
  - Detects `longPress` in the bottom 88dp (above nav bar).
  - On long press, shows a "pull-up for settings" overlay with a bounce animation.
  - If a swipe-up follows, opens the settings panel.
- **Trade-off**: High maintainability as all global gestures are in one place. Less likely to have conflicting consumption between nested components.

# Implementation 2: Decoupled Multi-Host

This approach reuses and extends the existing `Host` pattern.

- **Architecture**:
  - `IdleCalendarHost`: Updated to restrict trigger area to `88.dp` (instead of half screen).
  - `SettingsPullHost`: A new host wrapping the content *inside* `IdleCalendarHost`.
- **Settings Logic**:
  - Uses `Box` to layer a tiny, invisible trigger region at the bottom.
  - Implements the "bounce" text using `AnimatedVisibility` and a `repeatable` animation.
- **Trade-off**: Better separation of concerns. `IdleCalendarHost` remains focused only on the calendar. However, nesting pointer inputs can sometimes lead to "stutter" if not handled with `PointerEventPass.Initial`.
