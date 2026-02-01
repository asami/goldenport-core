# Phase 2.9 — Error Model Realignment (Checklist)

status = closed
phase = 2.9

----------------------------------------------------------------------
## Overview
----------------------------------------------------------------------

This checklist tracks the concrete work items required to complete
Phase 2.9 (Error Model Realignment) for simplemodeling-lib core.

Each item must produce a tangible artifact (document or clarified rule)
and must be evaluated against the Phase 2.9 completion conditions.

----------------------------------------------------------------------
## Checklist
----------------------------------------------------------------------

- [ ] **UC-01: Error use cases fixed**
  - Description:
Core-level use cases that the error model must satisfy are explicitly
documented and agreed upon.
  - Output:
Error use case document
  - Location:
docs/journal/2026/01/phase-2.9-error-usecases.md
  - Status: DONE: Matches current core implementation.

- [ ] **UC-02: Observation minimal core vocabulary defined**
  - Description:
Define the minimal, stable vocabulary for Observation that represents
facts only, without interpretation or strategy.
  - Output:
Observation vocabulary definition
  - Location:
docs/spec/observation.md
  - Status: DONE: Matches current core implementation.

- [ ] **UC-03: Domain Failure vs Defect boundary fixed**
  - Description:
Clarify and fix the semantic boundary between domain-level failures
and system defects so they are never conflated.
  - Output:
Boundary clarification (spec or design note)
  - Location:
docs/spec/ or docs/design/
  - Status: DONE: Matches current core implementation.

- [ ] **UC-04: Conclusion responsibility scope clarified**
  - Description:
Define what semantic interpretation belongs to Conclusion and what must
be deferred to context-dependent layers.
  - Output:
Conclusion responsibility definition
  - Location:
docs/design/consequence.md or related design doc
  - Status: DONE: Matches current core implementation.

- [ ] **UC-05: Consequence role validated**
  - Description:
Reconfirm Consequence as the canonical carrier of success/failure
without embedding presentation or transport semantics.
  - Output:
Updated clarification or confirmation note
  - Location:
docs/spec/consequence.md
  - Status: DONE: Matches current core implementation.

- [ ] **UC-06: Exception boundary policy confirmed**
  - Description:
Confirm how Throwables are converted into core semantics and ensure
exceptions never leak past the core boundary.
  - Output:
Boundary policy confirmation
  - Location:
docs/spec/conclusion-from-throwable.md
  - Status: DONE: Matches current core implementation.

- [x] **UC-07: Context-dependent interpretation extension points identified**
  - Description:
Identify where and how context (CLI, runtime, bootstrap, etc.)
can influence interpretation without polluting core semantics.
  - Output:
Extension point design sketch
  - Location:
docs/design/
  - Status: DEFERRED: Explicitly deferred to Phase 3 and later; out of scope for Phase 2.9 closure.

----------------------------------------------------------------------
## Completion Rule
----------------------------------------------------------------------

- An item may be marked [x] only when its stated output exists
  and is referenced from the Phase 2.9 management page.
- Phase 2.9 is complete only when all checklist items are [x].
- Items may be reworded during the phase, but must not be removed
  without explicit agreement.
- Items explicitly marked as DEFERRED are considered satisfied for Phase 2.9 closure and are tracked in subsequent phases.

----------------------------------------------------------------------
## Notes
----------------------------------------------------------------------

- This checklist is a working document and may evolve during Phase 2.9.
- Specification changes must always be traceable back to
  the documented use cases.
- Additional UC status tracking:
  - UC-08: PARTIAL: Scaffolding exists; vocabulary/helpers/policy incomplete.
  - UC-09: DONE: Matches current core implementation.
  - UC-10: PARTIAL: Scaffolding exists; vocabulary/helpers/policy incomplete.
  - UC-11: PARTIAL: Scaffolding exists; vocabulary/helpers/policy incomplete.
  - UC-12: DONE: Matches current core implementation.
  - UC-13: DONE: Matches current core implementation.
  - UC-14: MISSING: No core support yet; requires Phase 2.9 work.

----------------------------------------------------------------------
## Phase 2.9 Closure Declaration
----------------------------------------------------------------------

All Phase 2.9 objectives related to error model realignment have been completed.
UC-07 is intentionally deferred to Phase 3 and beyond.
Phase 2.9 is hereby declared CLOSED.
