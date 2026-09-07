# ADR 0002 — Deterministic Alarm Engine, Optional Agent Above It

## Status
Proposed

## Context
The project may later include AI/agent planning loops. Alarm delivery and recurrence are reliability-critical and must continue working offline and after process death.

## Decision
The deterministic task/scheduling engine owns task state, occurrence calculation, AlarmManager integration and reminder escalation. AI may only propose plans through validated application interfaces.

## Consequences
- alarms do not depend on model availability;
- AI can be removed without breaking core product;
- agent output must use typed/structured proposals;
- some “fully autonomous” behaviour is intentionally constrained.
