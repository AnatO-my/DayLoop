# Context Manifest — Phase 10A

**Phase brief:** `../phases/10a-projects-goals.md`

## Always read
- `../STATE.md`
- `../phases/10a-projects-goals.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- ADR 0003
- PROJECTS_AND_GOALS.md relevant model/protocol sections
- core/projects + persistence
- feature/plans
- Today provenance integration + tests

## Read only if a specific decision blocks work
- UPDATES_AND_UPGRADES.md if schema migration is required
- LOOPS.md project protocol section only

## Do not load or modify by default
- AI/calendar
- alarm platform internals except scheduler interface contract
- unrelated Today redesign

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
