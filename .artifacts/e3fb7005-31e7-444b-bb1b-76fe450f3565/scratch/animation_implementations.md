# Implementation 1: Spatial Continuity & Fluid Reordering

This approach leverages high-level Compose 1.7 APIs to create a physical sense of space.

### 1. Shared Element Transitions (Today -> Editor)
- **Architecture**: Wrap `DayLoopApp`'s `when(route)` block in a `SharedTransitionLayout`.
- **Logic**: Use `AnimatedContent` to switch between `today`, `plan`, and `editor`.
- **Mapping**: The `TodayTaskRow` uses `sharedElement` with a key based on `TaskId`. The `TaskEditorScreen`'s title field uses the same key.
- **Outcome**: Tapping a task causes the title and card to morph directly into the editor view.

### 2. Satisfying Completion (Strikethrough & Slide)
- **Strikethrough**: In `TodayTaskRow`, the title text is wrapped in a `Box` with a `Canvas` overlay. When `status == COMPLETED`, a path is drawn from 0 to 100% width using `animateFloatAsState`.
- **Reordering**: Add `Modifier.animateItemPlacement()` to every item in the `LazyColumn`. When a task is completed, it triggers a database update. The reactive `Flow` updates the list, and the completed item "slides" down to the completed section while others fill the gap.

---

# Implementation 2: Feedback-First Interaction Polish

This approach focuses on the "feeling" of interaction through micro-animations.

### 1. Hero-to-Editor Scale Transition
- **Architecture**: Use a custom `AnimatedContent` transition spec in `DayLoopApp` that scales the editor from the position of the tapped item (using a calculated offset).
- **Outcome**: A "zoom-in" effect from the tapped task, creating a similar spatial link without the complexity of `SharedTransitionLayout`.

### 2. High-Feedback Completion (Pop & Strike)
- **Strikethrough**: Use `AnnotatedString` with an animated `TextDecoration`.
- **Reordering**: Use `AnimatedVisibility` inside the `LazyColumn` for items. When completed, the item performs a "shrink" and "fade" exit animation, and a matching item "slides in" to the completed section.
- **Feedback**: Add a "haptic pop" and a subtle scale pulse (1.05f) to the task row the moment it's checked.
