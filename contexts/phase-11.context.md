# Context Manifest — Phase 11

**Phase brief:** `../phases/11-quality.md`

## Always read
- `../STATE.md`
- `../phases/11-quality.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- affected production modules only as audit findings require
- migration/performance/accessibility tests

## Read only if a specific decision blocks work
- UPDATES_AND_UPGRADES.md for real migrations/dependency upgrades
- accepted ADRs relevant to a discovered issue

## Do not load or modify by default
- Feature expansion
- AI/calendar unless part of defined beta scope

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
