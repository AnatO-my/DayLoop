# Phase 4 — Today UI / Calm Pressure Design System

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Create the signature Today screen while keeping canonical state outside Compose.

## Prerequisites
- TodaySnapshot is deterministic and tested.

## Concepts to learn or reuse
- Compose state hoisting (Level A).
- Unidirectional UI data flow/ViewModel state (Level A).
- Material 3 theming/design tokens and accessibility semantics (Level A).

## Implementation slices
- Create Today screen scaffold.
- Add progress/day-state header.
- Render overdue/now/next/anytime/completed sections.
- Add safe completion interaction.
- Create Day Complete/empty states.
- Implement Calm Pressure theme tokens and accessibility pass.

## Expected areas to change
- feature/today/
- core/design/
- ViewModel/UI state
- UI tests

## Verification
- State rendering tests where practical.
- Completion event path.
- Large font/accessibility semantics.
- No project dashboard on landing.

## Definition of done
A user can understand the remaining day at a glance and complete tasks from Today.

## Handoff
Next: **Phase 5 — Task Creation and Time Semantics**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
