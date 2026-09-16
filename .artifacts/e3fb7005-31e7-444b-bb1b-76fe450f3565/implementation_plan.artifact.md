# DayLoop: Non-Material Design System Refactor

Reconstruct the app's visual foundation to be entirely independent of Google Material, adopting the high-contrast, geometric aesthetic of Reference 2.

## User Review Required

> [!IMPORTANT]
> - **Architectural Shift**: We are moving away from `androidx.compose.material3`. This means components like `Text`, `Surface`, and `Button` will be replaced with custom `DayLoop` versions.
> - **Visual Identity**: The app will strictly follow the "Ref 2" look: sharp dark backgrounds, matcha accents, and unique "cut-out" card shapes.
> - **Phased Migration**: This plan initiates the foundation. Full screen migrations will follow in subsequent steps.

## Proposed Changes

### 1. Design System Foundation

#### [NEW] [DayLoopDesignSystem.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/ui/designsystem/DayLoopDesignSystem.kt)
- Implement `DayLoopColors`, `DayLoopTypography`, and `DayLoopShapes`.
- Define `MatchaCard` custom geometric shape.
- Create `DayLoopText` and `DayLoopBox` primitives using `BasicText` and `foundation` packages only.
- Implement `DayLoopTheme` as the new root provider.

### 2. Layout & Structure

#### [NEW] `DayLoopScaffold.kt`
- Create a custom scaffold that manages the top bar, content area, and the new Matcha-style bottom navigation without Material dependencies.

### 3. Proof of Concept

#### [MODIFY] `MatchaDesignSystemPreview`
- Provide a high-fidelity preview showing a sample "Today Task" card using the new system to verify the Ref 2 aesthetic.

## Verification Plan

### Automated Tests
- Gradle build to ensure no conflicting dependencies.
- Component-level previews for each new primitive.

### Manual Verification
- Visual inspection of the `MatchaDesignSystemPreview` to confirm alignment with Reference 2.
- Verify typography scaling and color contrast.
