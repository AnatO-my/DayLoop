# Context Manifest — Phase 14

**Phase brief:** `../phases/14-beta-release.md`

## Always read
- `../STATE.md`
- `../phases/14-beta-release.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- release configuration
- policy/privacy/onboarding docs
- migration/device test records

## Read only if a specific decision blocks work
- relevant accepted ADRs
- SOURCES.md/current official policy only where release verification requires it

## Do not load or modify by default
- New product features
- large refactors not required for release blockers

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.
