# Token-Efficient Development Protocol

This document defines how DayLoop should minimise AI context and response-token usage **without sacrificing correctness, testing, or learning**.

The optimisation target is not the fewest possible tokens. It is the **minimum sufficient context per verified increment**.

> Repository state carries memory. Conversation history should not be required to resume development.

---

## 1. Core rule

For each task, load only:

1. `AGENTS.md` as the standing repository contract;
2. `STATE.md`;
3. the current file under `phases/`;
4. the matching file under `contexts/`;
5. `LEARNED_CONCEPTS.md`;
6. only source/test/docs explicitly named by the current context manifest or directly implicated by the requested change.

Do **not** automatically read the full `PROJECT_PHASES.md`, `ARCHITECTURE.md`, `FEATURE_SPEC.md`, `LOOPS.md`, historical phase briefs, future-phase documents, or the entire source tree.

If the manifest proves insufficient, expand context one dependency at a time and state why.

---

## 2. Context manifest is authoritative for a phase

Every phase has a compact context file such as:

```text
contexts/phase-06.context.md
```

It contains:

- required documents;
- source/test areas that may be inspected;
- references that are optional;
- explicit areas that should not be loaded or modified.

A coding agent must not perform a broad repository scan merely to “understand the project” when the manifest already supplies the required boundary.

---

## 3. Phase briefs replace repeated roadmap loading

`PROJECT_PHASES.md` remains the master roadmap. Normal execution uses the smaller files in `phases/`.

A phase brief contains only:

- objective;
- prerequisites;
- new concepts;
- implementation slices;
- files/areas expected to change;
- tests;
- definition of done;
- next-phase handoff.

Read the master roadmap only when resolving a contradiction, changing phase boundaries, or planning across multiple phases.

---

## 4. `STATE.md` is compressed project memory

`STATE.md` should stay short. It records only facts required to resume work:

```text
Current phase/task
Last verified checkpoint
Implemented capabilities
Accepted decisions relevant now
Known failures/blockers
Files currently in play
Next single action
```

Do not turn `STATE.md` into a changelog. Historical detail belongs in Git history, ADRs, phase completion notes, or issue/task history.

At the end of a verified task, update `STATE.md` before ending the session.

---

## 5. `LEARNED_CONCEPTS.md` prevents repeated teaching

Guided Build remains the default, but a concept should be taught deeply only the first time.

Use three teaching levels:

### Level A — new foundational concept

Teach enough for the user to understand and later debug it.

Typical examples:

- Kotlin sealed hierarchies;
- Room entities/DAOs/migrations;
- Coroutines and `Flow`;
- Compose state and state hoisting;
- `AlarmManager`, `PendingIntent`, `BroadcastReceiver`;
- recurrence/time-zone algorithms;
- Project protocol generation;
- agent validation boundaries.

Normal explanation target: roughly **250–600 words**, plus a small design/pseudocode preview when useful.

### Level B — new application of a learned pattern

Explain only what is new or different.

Normal explanation target: roughly **80–200 words**.

### Level C — mechanical/repeated work

Give a one-to-three sentence explanation, then generate it.

Examples: repeated mappers, previews, test fixtures, routine Gradle entries after the pattern is understood.

After a Level A concept is taught and implemented, mark it in `LEARNED_CONCEPTS.md`. Future phases should normally downgrade repeated coverage to Level B or C.

---

## 6. Teach components, not individual files

Group files that implement one concept.

Prefer:

```text
Teach Task Domain
→ preview Task + TaskSchedule + TaskStatus + ReminderPolicy
→ implement the coherent component
→ test it
```

over four separate teach/write cycles.

The teaching unit should match the conceptual boundary, not the file count.

---

## 7. Do not echo files already written

When a tool writes or edits source files, the final response should normally report:

```text
Files changed:
Behaviour:
Tests:
Key concept:
Remaining risk:
Next action:
```

Do not print a complete 300-line file into chat unless the user explicitly asks to study or review the entire file.

For small code walkthroughs, quote only the relevant function/interface or point to file/line locations when available.

---

## 8. Prefer narrow patches over rewrites

When modifying working code:

- inspect the smallest relevant range;
- preserve accepted interfaces unless the feature requires a change;
- patch only the necessary files;
- avoid opportunistic refactors;
- do not regenerate whole modules for a three-line change.

A broad refactor requires its own explicit task and context budget.

---

## 9. Vertical slices reduce total usage

Build one end-to-end capability at a time where practical.

Example:

```text
Create task
→ persist task
→ observe task
→ display Today
→ complete task
→ verify
```

A verified vertical slice is preferable to partially implementing every layer. If a usage cap interrupts work, the repository remains in a valid checkpoint.

---

## 10. Output discipline

Default agent responses during implementation should be compact.

### Before writing

Provide only:

- component purpose;
- one concise architecture/data-flow explanation;
- new concepts and their teaching level;
- exact files to change;
- tests to run.

### During failures

Show only failing tests/errors relevant to the current change. Do not dump complete Gradle logs unless requested.

### After writing

Use the completion packet:

```text
Task completed:
Files changed:
Behaviour added/fixed:
Tests: <pass/fail summary>
New concept learned:
Remaining risks:
STATE/TASKS/LEARNED_CONCEPTS updated: yes/no
Next single action:
```

Stop after the scoped task or phase checkpoint. Do not automatically continue into the next phase.

---

## 11. Testing output should be summarised

Preferred successful report:

```text
Unit tests: 36 passed
Instrumentation: 5 passed
Build: PASS
Lint: PASS
```

On failure, report only:

- failing test name;
- expected vs actual when useful;
- short error excerpt;
- suspected scope.

Full raw output belongs in logs, not the conversation.

---

## 12. Research once, record once

For changing Android behaviour, current official documentation may need web verification.

After a consequential research decision:

1. record the conclusion in the relevant ADR or compact research note;
2. record the verification date/source;
3. do not repeat the research every session.

Re-check only when:

- `targetSdk`/platform requirements change;
- a dependency/API is being upgraded;
- the recorded note is stale for the current decision;
- new evidence materially conflicts with an accepted decision.

Accepted ADRs should not be reopened casually.

---

## 13. Scope controls

Unless the user explicitly requests a broad audit, do not:

- inspect every file;
- redesign unrelated components;
- propose unrelated future features during implementation;
- reread completed phase briefs;
- reread all ADRs;
- reopen accepted architectural choices;
- refactor working code “for cleanliness” outside scope;
- advance automatically to the next phase.

If a new idea appears during implementation, add a short backlog note and return to the current task.

---

## 14. Safe expansion rule

Minimal context must not become insufficient context.

If implementation cannot be done safely with the manifest:

```text
1. identify the missing fact;
2. name the smallest additional file/range needed;
3. read it;
4. continue;
5. update the context manifest later if this dependency is permanent.
```

Do not guess merely to save tokens.

---

## 15. Session restart protocol

A fresh AI session should be able to resume with this request:

```text
Resume DayLoop from the repository.
Read AGENTS.md, STATE.md, the current phase brief and its context manifest,
and LEARNED_CONCEPTS.md. Do not scan unrelated files.
Continue only the Next single action recorded in STATE.md using Guided Build.
Stop after verification and update the handoff files.
```

No previous conversation transcript should be necessary.

---

## 16. Phase completion packet

When a phase exits its gate, record a compact handoff:

```text
PHASE <n> COMPLETE

Built:
Verified:
Accepted decisions:
Concepts newly learned:
Known limitations:
Next phase:
```

Then update:

- `STATE.md`;
- `TASKS.md`;
- `LEARNED_CONCEPTS.md`;
- ADRs only if a decision actually changed.

Do not produce a long narrative recap unless requested.

---

## 17. Definition of token-efficient success

A phase is token-efficient when:

- the agent loaded only context required for the active slice;
- the user still learned every new foundational concept;
- no correctness/testing step was skipped;
- the repository itself contains enough state to resume later;
- responses did not echo large files/logs unnecessarily;
- accepted decisions were not repeatedly re-litigated;
- work stopped at a clean verified checkpoint.
