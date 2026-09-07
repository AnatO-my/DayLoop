# Updates and Upgrades Guide

This document defines how DayLoop should evolve after the first implementation begins.

The goal is to make additions deliberate, testable, educational, and safe for existing user data. New features should not be bolted directly onto the UI simply because they sound useful.

---

## Token-efficient upgrade context

Do not load the whole historical architecture before every update. Start from:

1. `STATE.md`;
2. the current phase brief/context manifest;
3. the exact existing feature files being changed;
4. the accepted ADR that governs that feature, if any;
5. only the relevant section of this guide.

For upgrades, compare **old behaviour → intended behaviour → migration/compatibility risk** rather than re-explaining the entire subsystem. Once an upgrade decision is recorded in an ADR/migration note, future sessions should treat it as compressed project memory.

See `TOKEN_EFFICIENCY.md`.

## 1. Types of change

Classify every proposed change before coding.

### A. Patch
Fixes behaviour without intentionally changing the product model.

Examples:
- duplicate alarm fires;
- wrong Today ordering;
- crash on empty recurrence rule.

### B. Feature addition
Adds a new capability within existing architecture.

Examples:
- Today widget;
- project linking;
- new snooze preset.

### C. Product-model evolution
Adds or changes a core concept.

Examples:
- introducing Projects;
- introducing Goals;
- adding phase-based study protocols;
- changing acknowledgement semantics.

These usually require architecture/domain review and possibly an ADR.

### D. Data/schema migration
Changes persisted representation.

Examples:
- new Room table;
- splitting schedule data into a new relation;
- adding project provenance to occurrences.

### E. Platform/dependency upgrade
Changes Android SDK, Kotlin, Compose, Room, Hilt, Gradle or another external dependency.

### F. Behavioural-policy upgrade
Changes how alarms, permissions, escalation, privacy or user control work.

These require current Android/Play policy verification.

---

## 2. Mandatory evolution loop

Every non-trivial update follows:

```text
PROPOSE
  ↓
CLASSIFY CHANGE
  ↓
TEACH USER
  ↓
CHECK PRODUCT FIT
  ↓
MODEL DOMAIN IMPACT
  ↓
CHECK DATA MIGRATION
  ↓
CHECK SCHEDULING/LIFECYCLE IMPACT
  ↓
WRITE/UPDATE ADR IF NEEDED
  ↓
IMPLEMENT SMALLEST SLICE
  ↓
TEST OLD + NEW BEHAVIOUR
  ↓
MIGRATE/ROLL OUT
  ↓
UPDATE STATE + DOCS
```

Read `BUILD_LEARNING_PROTOCOL.md` before implementation. The teaching step is not optional by default.

---

## 3. Feature proposal template

Before coding a feature, write a small proposal:

```text
Feature:
User problem:
Why a normal Task cannot already solve it:
Expected behaviour:
Today-screen impact:
Domain concepts introduced/changed:
Persistence impact:
Alarm/scheduling impact:
Migration required:
Privacy/permission impact:
Tests required:
Potential simpler alternative:
ADR required: yes/no
Target phase:
```

This prevents feature creep and UI-first design.

---

## 4. Example evolution — Projects and Goals

### User need
A user follows a study protocol:

```text
study 3 days
rest 3 days
repeat
```

### Bad upgrade
Add six manually generated tasks to the calendar for every cycle forever.

Problems:
- creates noise;
- rest days look like missing tasks if modelled badly;
- editing the pattern becomes difficult;
- no project context;
- infinite pre-generation creates migration/sync complexity later.

### Better upgrade
Introduce a Project protocol:

```text
Goal
  Master circuits

Project
  Circuits Spaced Study

Protocol
  ActiveStudyPhase(3 days)
  RestPhase(3 days)
  repeat

Derived today occurrence
  Study circuits for 60 minutes
```

The rest phase generates no required task.

Read `PROJECTS_AND_GOALS.md` for the full model.

---

## 5. Compatibility rule

An update must answer:

> What happens to a user who already has data from the previous version?

Never assume an empty database outside prototypes/tests.

For schema changes:

1. document old schema;
2. define new schema;
3. write an explicit Room migration;
4. write a migration test using an old database fixture where practical;
5. verify existing tasks still produce the same Today/alarm behaviour;
6. only then remove obsolete compatibility code.

Avoid destructive migration in production merely to simplify development.

---

## 6. Behaviour compatibility

Database compatibility is not enough.

An upgrade may preserve rows but still change meaning.

Example:

Old behaviour:
`ACKNOWLEDGED` stops a persistent chain.

New behaviour:
Only `COMPLETED` stops the chain.

That is a product behaviour migration. Existing reminder policies may require:

- a version field;
- explicit migration to new semantics;
- user notice/choice;
- or preserving old behaviour for existing tasks.

Document this before changing code.

---

## 7. Scheduler compatibility

Any update affecting task timing must test stale scheduled events.

When a task/project rule changes:

```text
Persist new source-of-truth state
       ↓
Increment generation/version
       ↓
Reconcile desired occurrences
       ↓
Cancel obsolete known alarms
       ↓
New alarms registered
       ↓
Old signal arrives anyway?
       ↓
Receiver rejects stale generation
```

Assume stale `PendingIntent`s can occur.

---

## 8. Project protocol upgrades

Project rules should be versioned or generation-aware.

If the user changes:

```text
Study 3 / Rest 3
```

to:

```text
Study 2 / Rest 1
```

we need an explicit editing policy:

- restart cycle today;
- preserve current phase and apply next phase;
- recalculate from original anchor;
- choose a new anchor date.

Do not silently guess because the resulting Today tasks can differ significantly.

---

## 9. Dependency upgrade procedure

For Kotlin/Compose/Room/Hilt/Gradle/Android SDK updates:

1. read current official release/migration notes;
2. record current versions;
3. identify breaking/deprecated APIs;
4. upgrade the smallest coherent dependency set;
5. compile before refactoring unrelated code;
6. run unit tests;
7. run instrumentation tests touching lifecycle/alarm behaviour;
8. inspect generated manifest/permission changes;
9. record upgrade in `STATE.md` or changelog;
10. make a separate commit from feature work when possible.

Avoid combining a major toolchain upgrade with a large domain feature.

---

## 10. Android platform upgrade procedure

When target SDK changes, explicitly re-check:

- notification permission;
- exact alarm rules;
- full-screen intent eligibility;
- background execution limits;
- foreground-service restrictions if any are used later;
- exported receiver/component requirements;
- PendingIntent mutability rules;
- backup/data behaviour;
- battery optimisation implications.

Do not assume alarms that worked on an older Android target behave identically under a newer target.

---

## 11. Upgrade testing matrix

For major upgrades, test at least:

```text
Fresh install
Existing database → upgraded app
Existing alarm scheduled → app upgraded
Task edited after upgrade
Device reboot after upgrade
Timezone change after upgrade
Permissions denied
Permissions granted later
Persistent chain active during upgrade
Project cycle active during upgrade
```

Use representative API levels and at least one physical device before store release.

---

## 12. Feature flags / staged activation

For risky features, prefer a dormant capability or developer flag before making it default.

Candidates:

- Strict reminders;
- alarm-style full-screen surfaces;
- project-generated tasks;
- AI schedule proposals;
- external calendar writes.

The domain should not become permanently littered with temporary flags. Remove flags after the feature stabilises.

---

## 13. Documentation update matrix

When behaviour changes, consider these documents:

```text
PROMPT.md                 agent/product contract
FEATURE_SPEC.md           user-visible behaviour
ARCHITECTURE.md           boundaries/data flow
PROJECT_PHASES.md         implementation sequencing
LOOPS.md                  state machines/repeated behaviour
PROJECTS_AND_GOALS.md     project/goal semantics
BUILD_LEARNING_PROTOCOL.md educational workflow
STATE.md                  current reality
TASKS.md                  next executable work
ADRs                       costly/lasting decisions
```

Do not let code become the only documentation of a core behavioural rule.

---

## 14. Release-level versioning

A simple early scheme is sufficient:

```text
MAJOR.MINOR.PATCH
```

- PATCH — fixes with no intentional product-model change.
- MINOR — backward-compatible features.
- MAJOR — significant user-visible semantic or compatibility changes.

Android `versionCode` must always increase for releases.

Pre-1.0 development can move faster, but database migrations and behavioural compatibility still matter.

---

## 15. Rollback thinking

Before shipping a risky change, ask:

> If this fails, can the user return to a valid schedule without losing data?

Potential safeguards:

- transactional writes;
- generation IDs;
- migration backups in development/testing;
- export/import later;
- repair/reconcile action;
- feature flag;
- reversible user-approved AI proposals.

Never make a cloud/AI service the only location where a user's schedule can be reconstructed.

---

## 16. Upgrade definition of done

An update is complete only when:

- change class is understood;
- user has been taught the relevant new component before files are written;
- old behaviour is protected or intentionally migrated;
- schema migrations are tested when required;
- alarm/scheduling compatibility is verified;
- new tests cover the feature;
- docs/ADRs are current;
- `STATE.md` reflects reality;
- no obsolete scheduled event can silently override the new source of truth.
