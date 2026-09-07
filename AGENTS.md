# AGENTS.md — Repository Instructions for Coding Agents

This file is the primary operating contract for any AI coding agent working in the DayLoop repository.

## Read order — minimal context by default

Before changing code, do **not** read the whole repository documentation set. Use this order:

1. `STATE.md`.
2. The current compact brief under `phases/`.
3. The matching manifest under `contexts/`.
4. `LEARNED_CONCEPTS.md`.
5. Only source/tests/docs explicitly named by that manifest or directly implicated by the requested change.
6. The relevant specialised agent file under `agents/` only when that role is being used.

`PROJECT_PHASES.md`, `ARCHITECTURE.md`, `FEATURE_SPEC.md`, `LOOPS.md`, `PROJECTS_AND_GOALS.md`, `UPDATES_AND_UPGRADES.md`, `SOURCES.md`, and historical ADRs are **reference documents, not default context**. Read the smallest relevant section only when the current manifest or a concrete missing fact requires it.

Follow `TOKEN_EFFICIENCY.md`. If minimal context is insufficient, expand one dependency at a time and state why. Do not guess to save tokens.

## Source of truth

- Product behaviour: `FEATURE_SPEC.md` + accepted ADRs.
- Current implementation status: `STATE.md`.
- Work backlog: `TASKS.md`.
- Technical boundaries: `ARCHITECTURE.md` + ADRs.
- Teaching-before-write workflow: `BUILD_LEARNING_PROTOCOL.md`.
- Evolution/migration process: `UPDATES_AND_UPGRADES.md`.
- Project/Goal semantics: `PROJECTS_AND_GOALS.md`.
- Context/token policy: `TOKEN_EFFICIENCY.md` + current `contexts/` manifest.
- Teaching history: `LEARNED_CONCEPTS.md`.
- External technical facts: current official Android documentation.

If README prose disagrees with an accepted ADR, the ADR wins.

## Agent roles

Use the role appropriate to the task:

- `agents/PLANNER_AGENT.md` — decomposes phases/features into safe implementation slices.
- `agents/IMPLEMENTER_AGENT.md` — writes production code and tests.
- `agents/REVIEWER_AGENT.md` — reviews correctness, architecture and lifecycle behaviour.
- `agents/QA_AGENT.md` — attacks time, permission, lifecycle and device edge cases.

A single AI may perform multiple roles sequentially, but should switch mindset explicitly and not let implementation assumptions contaminate review.

## Repository invariants

Do not violate these without an ADR:

- Android-first native implementation.
- Kotlin + Compose.
- Room is task source of truth.
- DataStore is for preferences/settings, not the canonical task database.
- Alarm delivery is deterministic and independent of AI/network.
- WorkManager is not a substitute for exact user-facing alarm times.
- AlarmManager is not a general background-job mechanism.
- Today is the default landing surface.
- Projects and Goals remain secondary planning context; their generated work reaches Today as Tasks/Occurrences.
- Task timing semantics are represented explicitly.
- Time access is injected/testable.
- Stale alarm delivery is expected and handled safely.
- Notification/alarm permission denial is a normal state, not an exceptional crash condition.


## Anti-vibe-coding rule

The default operating mode is **Guided Build**. Before creating or substantially modifying a component, teach the user how that component works according to `BUILD_LEARNING_PROTOCOL.md`.

A tool call that writes files should normally be preceded by a visible pre-write explanation covering purpose, architecture position, key Kotlin/Android concepts, data flow, failure cases, tests, and the exact files to be changed.

Do not silently generate foundational architecture. Repetitive boilerplate may be generated more freely after the pattern has already been taught. The user may explicitly waive the teaching step for a specific scope; do not treat that as a permanent preference.

Use the teaching level recorded in `LEARNED_CONCEPTS.md`: Level A for new foundational concepts, Level B for a new application of a known concept, and Level C for mechanical/repeated work. Do not re-teach a Level A concept in full without a materially new reason.

## Context and output discipline

- Work on one verified slice at a time.
- Do not advance automatically to the next phase.
- Do not broadly scan the repository when a context manifest exists.
- Do not echo full files after writing them unless the user asks to study them.
- Summarise successful test/build output; show only relevant failures.
- Prefer narrow patches to full rewrites.
- Record new ideas in the backlog instead of expanding current scope.
- Keep `STATE.md` compact enough to serve as a session handoff.

## Coding standards

- Prefer immutable domain models.
- Prefer sealed interfaces/classes for mutually exclusive schedule types.
- Keep Android framework types out of pure domain code.
- Use `Instant`/timezone-aware types deliberately; document local-time recurrence semantics.
- Avoid raw epoch arithmetic in UI layers.
- Repository operations that change task + occurrence state should be transactional where consistency matters.
- Side effects behind interfaces.
- UI state derived from domain state; do not duplicate canonical completion/schedule state in Compose.
- Every notification action must be idempotent.

## Testing standards

For domain changes, unit tests are mandatory unless genuinely impossible.

For Android scheduling changes, include a combination of:

- pure calculation tests;
- receiver/service tests where feasible;
- instrumented device/emulator verification;
- a manual test note for behaviours Android emulators cannot reliably simulate.

Time-sensitive tests use a fake/injectable clock.

## Build discipline

After a change:

1. run the smallest relevant tests;
2. run module tests;
3. run lint/compile for affected modules;
4. run the project’s defined verification command before marking complete.

If verification is slow, do not skip it silently; record what ran and what did not.

## Documentation discipline

Update documentation when:

- a public domain interface changes;
- a permission/capability becomes required;
- alarm behaviour changes;
- a new module boundary is introduced;
- a product invariant changes;
- an important trade-off is made.

Use an ADR for irreversible or high-cost architecture decisions.

## External-source discipline

Reference projects are for studying patterns and trade-offs. Do not wholesale copy code.

Always inspect licence compatibility before adapting implementation from a repository. In particular, reference repositories may be GPL or non-commercially licensed.

Prefer official Android documentation over blog posts for background execution, notifications, alarms and permissions.

## AI feature discipline

If working in the optional agent phase:

- LLM output is untrusted input.
- Require structured schemas.
- Validate against domain constraints.
- Do not grant an LLM direct access to `AlarmManager` or database DAOs.
- Route writes through use cases/repositories.
- Make proposals reversible.
- Preserve a fully non-AI workflow.

## Completion format

At the end of a coding task, first give a short post-write explanation of the component and what concept the user should now understand. Then report:

```text
Task completed:
Files changed:
Behaviour added/fixed:
Tests: <summary>
New concept learned:
Remaining risks:
STATE/TASKS/LEARNED_CONCEPTS updated: yes/no
Next single action:
```

Do not paste full generated files or raw successful build logs unless requested.
