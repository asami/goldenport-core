# Goldenport Design Reference

This directory documents the design intent, semantic model, and proven idioms
of the goldenport framework.

The purpose of this documentation is NOT to port goldenport as-is,
but to extract and preserve its architectural knowledge so that it can be:

- reinterpreted in Scala 3
- aligned with cats
- incrementally adopted in SimpleModeling core, CNCF, and CLI

Goldenport is treated as a reference architecture, not a dependency.

----------------------------------------------------------------------  
Why Goldenport Matters
----------------------------------------------------------------------

Goldenport is a long-lived framework that has accumulated
practical design knowledge across:

- CLI argument parsing
- record / schema modeling
- datatype validation
- semantic error handling
- configuration and execution safety

Many of its ideas are structurally sound,
even if the original implementation is:

- Scala 2–based
- Scalaz-oriented
- tightly coupled and large

This document set aims to separate the ideas from the code.

----------------------------------------------------------------------  
Design Philosophy Extracted from Goldenport
----------------------------------------------------------------------

The following principles repeatedly appear across goldenport:

- Errors are values, not control flow
- Validation and execution share a unified error model
- Schemas are declarative, values are validated later
- Records carry values, schemas carry structure
- CLI, config, and runtime share the same semantic foundations
- Error handling must be:
  - composable
  - explainable
  - safe for both humans and machines

These principles are directly relevant to:

- Consequence
- Conclusion
- Schema / Column
- Datatype
- CLI validation
- CNCF execution

----------------------------------------------------------------------  
Scope of This Reference
----------------------------------------------------------------------

This documentation focuses on conceptual and semantic aspects.

Included:
- Consequence / Conclusion design
- Schema / Column / ValueDomain concepts
- Datatype and validation semantics
- Error and fault classification
- CLI and configuration idioms

Explicitly excluded:
- Full API surface
- Scalaz-specific implementation details
- Legacy JSON / Play / tracing integrations

----------------------------------------------------------------------  
Relationship to SimpleModeling
----------------------------------------------------------------------

Goldenport concepts are reused in SimpleModeling as follows:

- core
  - minimal, declarative representations
  - unified error and validation model

- mini CLI
  - early validation using schema / datatype
  - prevention of invalid or malicious input

- CNCF
  - extended semantics
  - runtime-oriented behavior
  - domain-specific enrichment

Goldenport knowledge is pulled upward into core,
then extended downward in CNCF.

----------------------------------------------------------------------  
Documents in This Directory
----------------------------------------------------------------------

Each document captures one conceptual area:

- consequence.md
  - semantic execution result model

- conclusion.md
  - error classification and explanation

- schema.md
  - declarative structure modeling

- column.md
  - column-level validation and constraints

- datatype.md
  - datatype semantics and conversion

- validation.md
  - validation strategy and composition

- error-model.md
  - fault taxonomy and error unification

- migration-notes.md
  - guidance for incremental adoption

Each document is written as a design reference, not an API manual.

----------------------------------------------------------------------  
How to Use This Reference
----------------------------------------------------------------------

When implementing or modifying code in:

- SimpleModeling core
- CNCF
- CLI / tooling

follow this rule:

Always identify which goldenport concept is being applied,
and reference the corresponding document.

Example:

  // Based on goldenport/consequence.md §Applicative Composition

This ensures that:
- design intent is preserved
- future refactoring remains coherent
- AI-assisted development remains aligned

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This reference is living documentation.

- It will grow incrementally
- It does not need to be complete up front
- Accuracy and intent matter more than coverage

Goldenport knowledge is treated as a long-term asset.
