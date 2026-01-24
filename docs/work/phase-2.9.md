# Phase 2.9 — Error Model Realignment (Core)

status = active
phase = 2.9

----------------------------------------------------------------------
## Purpose
----------------------------------------------------------------------

Phase 2.9 focuses on realigning the core error model of simplemodeling-lib
based on accumulated empirical usage and clarified architectural intent.

The goal of this phase is NOT to add features, but to:
- Fix semantic boundaries
- Clarify responsibilities
- Stabilize core concepts before further expansion

This phase is driven by concrete use cases observed in:
- CNCF runtime and configuration loading
- CLI execution paths
- Bootstrap and initialization failure handling

----------------------------------------------------------------------
## Scope
----------------------------------------------------------------------

In scope:

- Core error abstractions:
  - Observation
  - Conclusion
  - Consequence

- Semantic separation of:
  - Domain Failure
  - Configuration Error
  - System Defect

- Clarification of what belongs to:
  - simplemodeling-lib core
  - Higher-level frameworks (e.g. CNCF, SIE)
  - Adapters / projections (CLI, HTTP, etc.)

- Vocabulary stabilization at the Observation level

Out of scope:

- HTTP / Web status mapping
- CLI presentation rules
- Logging / observability backend behavior
- Framework-specific convenience APIs

----------------------------------------------------------------------
## Current Work A
----------------------------------------------------------------------

Define and fix the core error model semantics by grounding them
in explicit use cases, and establish a stable foundation for
Observation / Conclusion / Consequence in simplemodeling-lib core.

----------------------------------------------------------------------
## Completion Conditions
----------------------------------------------------------------------

Phase 2.9 is considered complete when:

- Error-related use cases are explicitly documented and agreed upon
- Observation minimal core vocabulary is fixed
- The boundary between Domain Failure and Defect is unambiguous
- Conclusion responsibilities are clearly delimited
- Extension points for context-dependent interpretation are identified
- No framework- or transport-specific semantics leak into core specs

----------------------------------------------------------------------
## Key References
----------------------------------------------------------------------

Journal:

- docs/journal/2026/01/phase-2.9-error-usecases.md

Design / Specification:

- docs/spec/observation.md
- docs/spec/consequence.md
- docs/spec/conclusion-from-throwable.md
- docs/design/consequence.md
- docs/design/protocol-core.md

Notes / Exploration:

- docs/notes/error-model-exploration.md
- docs/notes/core-error-semantics.md
- docs/notes/defect-vs-domain-failure.dox

----------------------------------------------------------------------
## Working Rule
----------------------------------------------------------------------

- This document is mutable during Phase 2.9
- Specs and designs must not be modified without referencing
  the agreed use cases captured in the journal
- Any ambiguity must be resolved at the semantic level,
  not deferred to presentation or transport layers
