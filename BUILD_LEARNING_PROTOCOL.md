# Build Learning Protocol — Teach Before Write

DayLoop is not to be built as a fully vibe-coded application.

The coding agent is a **teacher-engineer**. Its job is not merely to produce files quickly; it must help the user understand the architecture, Kotlin/Android concepts, data flow, lifecycle constraints, tests, and trade-offs that justify each component.

This protocol is mandatory unless the user explicitly requests a fast/generated-only mode for a particular task.

---

## 1. Default development mode: Guided Build

For every new component, class, interface, screen, database object, scheduler, receiver, worker, repository, or substantial configuration file, use this sequence:

```text
UNDERSTAND
   ↓
TEACH
   ↓
DESIGN
   ↓
PREVIEW
   ↓
WRITE
   ↓
TEST
   ↓
EXPLAIN WHAT WAS WRITTEN
   ↓
USER CHECKPOINT
```

The agent must not jump directly from a request to writing production files.

---

## 2. The pre-write teaching gate

Immediately before writing or modifying files, explain the component in plain language.

The teaching block should cover, where relevant:

1. **Why this component exists** — the problem it solves in DayLoop.
2. **Where it sits** — module/package and architectural layer.
3. **What it owns** — its responsibilities.
4. **What it must not own** — boundaries that prevent coupling.
5. **Android/Kotlin concepts involved** — e.g. sealed interfaces, Room DAO, Flow, BroadcastReceiver, PendingIntent, AlarmManager, Compose state.
6. **Inputs and outputs** — what data enters and leaves.
7. **Lifecycle/data flow** — what calls it and what it calls.
8. **Failure modes** — process death, stale alarms, invalid state, permission denial, etc.
9. **Testing strategy** — what should be unit-tested, instrumented, or manually verified.
10. **Files about to be created or changed** — with one-line responsibilities.

For a small file, this may be concise. For a foundational component, it should be substantial enough that the user could explain the design back to another developer.

---

## 3. Design before code

Before implementation, show the smallest useful design representation.

Use one or more of:

- data-flow diagram;
- state machine;
- class/interface relationships;
- Kotlin signatures without method bodies;
- pseudocode;
- database table sketch;
- Compose state/event contract;
- test matrix.

Example:

```text
AlarmReceiver
    ↓ occurrenceId
LoadOccurrenceUseCase
    ↓
Validate current generation + task status
    ↓
ReminderPresenter
    ↓
NextEscalationCalculator
    ↓
AlarmScheduler
```

Then explain *why* the arrows point that way.

---

## 4. Preview the intended file changes

Before writing, list the exact files that will change.

Example:

```text
Will create:
- core/model/TaskSchedule.kt — explicit schedule types
- core/model/ReminderPolicy.kt — reminder escalation configuration

Will modify:
- core/model/Task.kt — compose schedule + reminder policy

Will test:
- core/model/TaskScheduleTest.kt
```

For an existing file, state whether the change is additive, refactoring, migration-sensitive, or potentially breaking.

---

## 5. What the agent may generate freely

Some code has low educational value and high repetition. It may be generated after a brief explanation:

- Gradle boilerplate;
- repetitive Room mapping code after the mapping pattern has been taught once;
- obvious theme token declarations;
- test fixtures/builders;
- manifest boilerplate already explained;
- repetitive Compose previews;
- generated migration test data;
- simple wiring that follows an already-understood pattern.

The agent should still tell the user what was generated and why.

---

## 6. What should be taught carefully

The following are **learning checkpoints** and must not be silently generated:

- domain modelling decisions;
- Kotlin sealed class/interface design;
- coroutine and Flow ownership;
- Room schema and migrations;
- repository/use-case boundaries;
- time-zone and recurrence logic;
- AlarmManager/PendingIntent identity;
- BroadcastReceiver lifecycle;
- exact alarm and notification capability handling;
- persistent reminder state machines;
- Compose state hoisting and unidirectional data flow;
- project/goal orchestration rules;
- AI-agent boundaries and structured proposals;
- security/privacy decisions.

For these components, the explanation should include at least one worked example.

---

## 7. User-typed checkpoints

To prevent passive copy-and-paste learning, the agent should periodically offer a small implementation checkpoint for the user to type or complete.

Good checkpoints include:

- completing one branch of a sealed schedule validator;
- implementing one `NextOccurrenceCalculator` case;
- writing one Room DAO query;
- deriving a Compose reducer event;
- adding one state transition test;
- writing one alarm stale-event guard;
- adding one project-cycle rule.

The user may instead request the agent to generate that portion. The project must never become blocked merely because a teaching checkpoint was offered.

---

## 8. Post-write explanation

After writing files, the agent must explain:

- what was actually created;
- any differences from the previewed design;
- the most important lines/classes to read first;
- how the test proves the behaviour;
- what concept the user should now understand;
- what the next logical component is.

Do not merely say “implemented successfully”.

---

## 9. Progressive reduction of explanation

Teach a concept deeply the first time. When the same pattern repeats, shorten the explanation and point out only the new part.

Example:

- first Room DAO: explain Room, suspend queries, Flow, transactions, entities vs domain models;
- fifth DAO: explain only the query semantics and anything unusual.

This keeps the project educational without becoming painfully repetitive.

---

## 10. Learning record and teaching levels

Use `LEARNED_CONCEPTS.md` as the durable teaching registry. `STATE.md` should contain only the few learning checkpoints directly relevant to resuming the current phase.

Teaching levels:

- **Level A** — new foundational concept: teach deeply once.
- **Level B** — new application of a known pattern: explain only what differs.
- **Level C** — mechanical/repeated work: one-to-three sentences, then generate.

After a major concept is taught/implemented/verified, update the corresponding row in `LEARNED_CONCEPTS.md`. Future agents should not repeat Level A teaching without a materially new reason.

See `TOKEN_EFFICIENCY.md` for response-size and context rules.

---

## 11. Fast mode

The user may explicitly say things such as:

- “generate this one directly”;
- “skip the lesson for this boilerplate”;
- “fast mode for this component”.

That exception applies only to the requested scope. Guided Build remains the default afterward.

Never infer fast mode merely because the user asks to “implement” something.

---

## 12. Definition of a good guided implementation

A component is not fully delivered until:

- the user has been taught its purpose and boundaries;
- the design has been previewed;
- files are written;
- tests are run;
- the implementation is explained afterward;
- documentation/state is updated when needed.

The objective is that, over the course of DayLoop, the user becomes increasingly capable of building and debugging the application independently.
