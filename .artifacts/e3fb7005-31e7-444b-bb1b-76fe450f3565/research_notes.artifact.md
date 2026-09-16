# Pomodoro Fullscreen Timer Research

## Existing Components
- `FocusScreen.kt`: Current implementation of the Pomodoro timer.
- `FocusStore.kt`: Manages timer state, modes, and durations.
- `DayLoopHeroCard`: The container used for the timer UI.
- `DayLoopProgressRing`: A circular progress component (currently simple).
- `MaterialTheme.dayLoopTokens`: Design tokens for spacing, shapes, and colors.

## Proposed Approaches

### Approach 1: State-driven Internal Overlay (The "Zoom" Effect)
- **Concept**: The `FocusScreen` manages a `isEnlarged` boolean.
- **UI**: When `isEnlarged`, a full-screen `Box` with the primary container color overlays the screen.
- **Animation**: Use `AnimatedContent` or `Box` with shared-element-like transitions (manual or via Compose 1.7+ Shared Elements if available, but staying safe with standard animations).
- **Navigation**: Uses `BackHandler` to intercept the back button and toggle the state.

### Approach 2: Full-screen Modal/Dialog with Circular Progress
- **Concept**: Use a `FullscreenDialog` or a full-screen `ModalBottomSheet` that triggers when clicking the timer name.
- **UI**: A dedicated Composable for the large circular progress ring.
- **Animation**: Standard modal slide-up or scale-up.
- **Navigation**: Naturally handles back button via the modal's internal behavior.

## User Constraints
- "utilizes an animated circular progress ring countdown timer"
- "Activating the backward navigation or back button would animate the fullscreen state back to the focus screen"
- "only when a timer is active" (must check `session.endsAt != null`)
- "short click on the timer name"

## Technical Considerations
- **Progress Calculation**: `(totalSeconds - remainingSeconds) / totalSeconds`.
- **Animation**: The circular progress should animate smoothly as seconds tick.
- **Back Button**: Must handle system back navigation.
