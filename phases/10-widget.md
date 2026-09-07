# Phase 10 — Home-Screen Widget and Quick Actions

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Expose Today state outside the app without creating a second source of truth.

## Prerequisites
- Today and completion paths are stable.

## Concepts to learn or reuse
- Android/Glance widget state projection (Level A).
- Safe widget actions/deep links (Level B).

## Implementation slices
- Project unresolved count + Now/Next into widget state.
- Render Today widget.
- Add safe quick completion/deep links.
- Add privacy setting.

## Expected areas to change
- widget/
- Today projection/use case
- settings
- widget tests

## Verification
- Widget follows Room/Today state.
- Quick action is idempotent.
- Privacy mode hides configured content.

## Definition of done
Widget remains consistent with source-of-truth state and supports fast daily viewing.

## Handoff
Next: **Phase 10A — Projects, Goals and Behavioural Protocols**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
