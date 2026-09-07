# Phase 12 — Optional Planning Agent

**Execution rule:** Read this file with `STATE.md`, the matching context manifest, and `LEARNED_CONCEPTS.md`. Do not load the master roadmap unless this brief is insufficient.

## Objective
Add useful planning intelligence while keeping all canonical scheduling deterministic.

## Prerequisites
- Deterministic core verified and usable with AI disabled.

## Concepts to learn or reuse
- Structured LLM proposal/validation boundary (Level B/A for implementation).
- Schema validation and reversible plan application (Level A).

## Implementation slices
- Define planner input snapshot/constraints.
- Define structured proposal schema.
- Implement deterministic validator.
- Add explicit user confirmation.
- Add overload/reordering/recovery proposals.
- Ensure offline/non-AI path remains complete.

## Expected areas to change
- agent/
- domain validation use cases
- planning UI
- tests

## Verification
- Invalid timestamps rejected.
- AI failure leaves schedule unchanged.
- User rejection writes nothing.
- Approved proposal routes through normal repository/scheduler.

## Definition of done
AI may improve a plan but cannot directly schedule alarms or mutate canonical state without validation/approval.

## Handoff
Next: **Phase 13 — Calendar and Optional Sync**. Update `STATE.md`, `TASKS.md`, and `LEARNED_CONCEPTS.md`, then stop.
