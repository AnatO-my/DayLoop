# Revert Design System Migration

Revert the "Non-Material Design System foundation" changes implemented on 2026-09-14, restoring the Material 3 based UI while preserving all mascot, AI, and functional improvements from 2026-09-13.

## User Review Required

> [!IMPORTANT]
> - **Architectural Revert**: We are moving back to `androidx.compose.material3` as the primary UI foundation. The `designsystem` package will be removed.
> - **Visual Consistency**: The "Matcha" visual identity (colors and icons) will be preserved through standard Android resources (`colors.xml`) and the original `DayLoopTheme`.
> - **Component Restoration**: `DayLoopChoice` and `DayLoopHeader` will be replaced with their predecessors (`DayLoopPillTabRow` and standard `Text` components).

## Proposed Changes

### 1. Cleanup

#### [DELETE] [DayLoopDesignSystem.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/ui/designsystem/DayLoopDesignSystem.kt)
- Remove the non-Material theme foundation.

### 2. Feature Reverts

#### [MODIFY] [AppearanceControls.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/diagnostics/AppearanceControls.kt)
- Revert `DayLoopChoice` usage to `DayLoopPillTabRow`.
- Remove `designsystem` import.

#### [MODIFY] [TaskEditorScreen.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/taskeditor/TaskEditorScreen.kt)
- Replace `DayLoopChoice` calls with `DayLoopPillTabRow`.
- Replace `DayLoopHeader` with standard `Text` using `MaterialTheme.typography.titleMedium`.
- Remove `designsystem` imports.

#### [MODIFY] [PlanningEditors.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/plans/PlanningEditors.kt)
- Replace `DayLoopChoice` with `DayLoopPillTabRow` or standard selection rows.
- Remove `designsystem` import.

#### [MODIFY] [TodayScreen.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/today/TodayScreen.kt)
- Revert `DayLoopTextField`, `DayLoopBox`, and `DayLoopText` in the Mascot Chat UI back to standard Foundation/Material versions.
- Revert the `TodayScreenPreview` to use the Material-based `DayLoopTheme`.
- Remove all `designsystem` imports.

#### [MODIFY] [FocusScreen.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/feature/focus/FocusScreen.kt)
- Remove `DayLoopHeader` and `DayLoopNavItem` (if used).
- Remove `designsystem` imports.

### 3. Core Component Restoration

#### [MODIFY] [DayLoopIcon.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/ui/components/DayLoopIcon.kt)
- Remove `LocalDayLoopColors` and restore `MaterialTheme.colorScheme` tokens for default tints.
- Remove `designsystem` import.

#### [MODIFY] [DayLoopNavigation.kt](file:///C:/Users/Ot/Downloads/Self%20Made%20Apps/DayLoop/app/src/main/java/com/dayloop/app/ui/components/DayLoopNavigation.kt)
- Remove `designsystem` imports.

## Verification Plan

### Automated Tests
- `./gradlew :app:compileDebugKotlin` to ensure all unresolved references are fixed and new ones aren't introduced.
- Run `MigrationTest` to ensure no accidental data model changes occurred.

### Manual Verification
- Verify the Appearance settings screen (More > Appearance) correctly applies palette and font changes.
- Verify the Task Editor and Planning dialogs render correctly with the restored `DayLoopPillTabRow` components.
- Verify the Mascot Modal and Chat UI function correctly.
