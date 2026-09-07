# Context Manifest — Phase 09

**Phase brief:** `../phases/09-recovery-diagnostics.md`

## Always read
- `../STATE.md`
- `../phases/09-recovery-diagnostics.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- alarm persistence/scheduler
- reconciliation receivers
- diagnostics feature + tests
- manifest

## Read only if a specific decision blocks work
- LOOPS.md recovery section only
- relevant accepted alarm ADRs

## Do not load or modify by default
- Projects/Goals
- AI/calendar
- unrelated UI refactors

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
