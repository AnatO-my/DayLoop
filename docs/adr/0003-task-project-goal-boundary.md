# ADR 0003 — Task, Project and Goal Boundary

- Status: Accepted
- Date: 2026-09-07
- Phase: 10A

## Context

DayLoop is Today-first, but some behaviours span many days and contain intentional non-task periods. Modelling these as large collections of manually scheduled tasks would create clutter and make editing difficult.

A representative case is a learning protocol with three active study days followed by three intentional rest days, repeated over a period.

## Proposed decision

Introduce three conceptual levels:

```text
Goal → Project → Task/Occurrence
```

- **Goal** represents a desired outcome and does not directly own alarm behaviour.
- **Project** represents an organised process, collection of work, or phase-based behavioural protocol.
- **Task/Occurrence** is the executable unit shown on Today and passed through the normal reminder/scheduler engine.

Project protocols may derive task occurrences, but they may not schedule platform alarms directly.

Intentional Project Rest phases generate no required task and do not reduce Today completion.

Projects and Goals live on a secondary `Plans` surface so Today remains uncluttered.

## Consequences

### Positive
- supports study/rest cycles and similar protocols naturally;
- avoids creating fake “do nothing” tasks;
- preserves Today as an execution surface;
- keeps platform scheduling centralised;
- gives long-term goals context without turning the app into a project dashboard.

### Costs
- adds new persistence/domain concepts;
- project-rule editing requires clear anchor/migration semantics;
- provenance is needed for generated occurrences;
- additional UI is required outside MVP task flow.

## Implementation timing

Do not implement until core task scheduling, recurrence, and alarm reconciliation are stable. See the Projects/Goals phase in `PROJECT_PHASES.md`.
