# Context Manifest — Phase 00

**Phase brief:** `../phases/00-product-spike.md`

## Always read
- `../STATE.md`
- `../phases/00-product-spike.md`
- `../LEARNED_CONCEPTS.md`

`AGENTS.md` is the standing repository contract and is assumed to govern the session.

## Read/inspect for this phase
- TASKS.md current Phase 0 entries
- PRODUCT_CONTRACT.md for Phase 0 product boundary and acceptance scenarios
- TASK_SEMANTICS_SCENARIOS.md for task timing model scenarios
- REMINDER_CONTRACT_SCENARIOS.md for reminder contract scenarios
- relevant `docs/adr/` only when recording a decision
- `docs/adr/0005-alarm-capability-policy.md` for accepted exact-alarm/full-screen reminder capability strategy
- `docs/adr/0004-android-version-policy.md` for Android version policy and future Android project setup
- `docs/spikes/p0-05-exact-alarm-spike.md` for exact alarm spike harness and results
- `spikes/exact-alarm/` only when checking or rerunning Phase 0 alarm/notification spike verification
- `docs/spikes/p0-06-alarm-ui-spike.md` for alarm UI spike harness and results
- `docs/spikes/p0-07-reboot-recovery-spike.md` for reboot recovery spike harness and results
- `docs/spikes/p0-08-interval-one-shot-spike.md` for interval one-shot spike harness and results
- `docs/spikes/p0-09-play-policy-review.md` for exact-alarm/full-screen-intent Google Play constraints

## Read only if a specific decision blocks work
- SOURCES.md Android alarm/policy references only when performing a platform spike
- UPDATES_AND_UPGRADES.md only if changing an accepted decision

## Do not load or modify by default
- Production feature modules
- Projects/Goals implementation
- AI/calendar/widget docs unless directly needed for product non-goals

## Expansion rule
If a missing dependency blocks safe implementation, name the missing fact and inspect the smallest additional file/range needed. Do not scan the repository broadly.

## Stop rule
Complete only the current `STATE.md` action or agreed slice, verify it, update handoff files, then stop.









