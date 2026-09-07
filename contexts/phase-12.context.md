# Context Manifest — Phase 12

**Phase brief:** `../phases/12-agent.md`

## Always read
- `../STATE.md`
- `../phases/12-agent.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- agent module
- TodaySnapshot/domain constraints
- repository/use-case write interfaces
- agent tests
- ADR 0002

## Read only if a specific decision blocks work
- ARCHITECTURE.md agent boundary section only

## Do not load or modify by default
- Direct AlarmManager/DAO access from LLM layer
- calendar/sync unless required by explicit agent input

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
