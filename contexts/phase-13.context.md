# Context Manifest — Phase 13

**Phase brief:** `../phases/13-calendar-sync.md`

## Always read
- `../STATE.md`
- `../phases/13-calendar-sync.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- calendar integration package
- task/planning projection interfaces
- settings/permissions
- sync package only if explicitly in scope

## Read only if a specific decision blocks work
- UPDATES_AND_UPGRADES.md for schema/account changes
- official Android calendar docs if current API facts matter

## Do not load or modify by default
- alarm internals
- AI refactors
- unrelated product features

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
