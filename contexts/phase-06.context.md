# Context Manifest — Phase 06

**Phase brief:** `../phases/06-alarm-engine.md`

## Always read
- `../STATE.md`
- `../phases/06-alarm-engine.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- task/occurrence persistence
- core/alarms
- core/notifications
- manifest + alarm tests
- ADR 0002

## Read only if a specific decision blocks work
- Phase 0 alarm ADR/research notes
- SOURCES.md official Android alarm refs only if platform fact must be re-verified
- LOOPS.md alarm loop section only

## Do not load or modify by default
- Today styling
- Projects/Goals
- widget
- AI/calendar

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
