======================================================================
Executable Specification Style — SimpleModeling Library
======================================================================

Status: Normative
Scope: simplemodeling-lib (Executable Specifications)

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document defines the normative style for Executable Specifications
in SimpleModeling. Executable Specifications are first-class
documentation and must be readable as specifications.

----------------------------------------------------------------------
2. Terminology
----------------------------------------------------------------------

- Executable Specification: ScalaTest specs under `src/test/scala`.
- Static Specification: Markdown documents under `docs/spec`.

Executable Specifications define authoritative behavior.
Static Specifications describe intent and context.

----------------------------------------------------------------------
3. Required Structure
----------------------------------------------------------------------

Executable Specifications MUST use:

- AnyWordSpec
- Matchers
- Given/When/Then
- Property-Based Testing as the primary technique

Given/When/Then is required. PBT is the baseline, not an optional add-on.

----------------------------------------------------------------------
4. Content Rules
----------------------------------------------------------------------

Executable Specifications MUST:
- describe semantic interpretation, not execution mechanics
- avoid example-only unit tests
- prioritize invariants and properties

Executable Specifications MUST NOT:
- assert private helper behavior
- duplicate implementation details

----------------------------------------------------------------------
5. Placement Rules (by Package)
----------------------------------------------------------------------

Executable Specifications are organized by semantic responsibility.

- org.goldenport.protocol
  - Protocol / Model semantics (semantic boundary)
  - datatype normalization, parameter resolution

- org.goldenport.scenario
  - usecase -> usecase slice -> BDD semantics
  - scenario specifications written as Given/When/Then

----------------------------------------------------------------------
6. Relationship to Static Specifications
----------------------------------------------------------------------

Static Specifications in `docs/spec` must remain consistent with
Executable Specifications. When behavior changes:
- update the Executable Specification first
- update the corresponding static specification

======================================================================
