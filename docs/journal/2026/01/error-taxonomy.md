# Error Taxonomy — Phase 2.9 Consolidated Draft

status = draft
phase = 2.9
kind = journal

----------------------------------------------------------------------
## Purpose
----------------------------------------------------------------------

This document records the consolidated error taxonomy agreed during
Phase 2.9.

Its goal is to establish a stable, interpretation-free classification
of errors at the Observation level, before fixing APIs, error codes,
or projection-specific behavior.

----------------------------------------------------------------------
## Core Principles
----------------------------------------------------------------------

- Error taxonomy classifies *what is factually wrong*, not how to react
- Classification must be stable across projections and runtimes
- Interpretation, severity, and handling are deferred to Conclusion
- Error classification and error provenance are orthogonal concerns

----------------------------------------------------------------------
## Axes of Error Description
----------------------------------------------------------------------

Error information is described along two independent axes:

1. Taxonomy (what is wrong)
2. Provenance / Source (where the data or failure originated)

This document defines the taxonomy only.
Provenance is treated as contextual metadata.

----------------------------------------------------------------------
## Top-Level Error Categories
----------------------------------------------------------------------

The core error taxonomy consists of the following top-level categories:

- argument
- property
- configuration
- resource
- state
- system

Each category represents a distinct class of factual failure.

----------------------------------------------------------------------
## Category: argument
----------------------------------------------------------------------

The `argument` category represents invalid or unexpected direct
input parameters supplied to an operation.

These errors are domain failures.

### argument Subcategories

- syntax_error
  The argument cannot be parsed as structured input.

- format_error
  The argument has a valid structure but an invalid representation
  (e.g. date format, encoding).

- domain_value
  The argument violates semantic or domain constraints
  (range, multiplicity, business rule).

- missing
  A required argument was not provided.

- unexpected
  An argument was provided that is not accepted or defined.

- unsupported
  The argument refers to a supported concept in an unsupported way.

----------------------------------------------------------------------
## Category: property
----------------------------------------------------------------------

The `property` category mirrors the `argument` category, but applies
to structured data fields rather than direct parameters.

Property errors may arise from entity I/O, external integrations,
or database conversions.

### property Subcategories

- syntax_error
- format_error
- domain_value
- missing
- unexpected
- unsupported

The classification describes the nature of the invalid data,
independent of its origin.

----------------------------------------------------------------------
## Category: configuration
----------------------------------------------------------------------

The `configuration` category represents invalid or inconsistent
system or component configuration.

Its subcategories mirror `argument` and `property`, but configuration
errors often escalate to system defects depending on context.

----------------------------------------------------------------------
## Category: resource
----------------------------------------------------------------------

The `resource` category represents factual problems related to
identifiable resources and their availability or integrity.

### resource Subcategories

- not_found
  A resource identified by a provided key or identifier does not exist.

- unavailable
  A resource exists but cannot be accessed or used at the moment.

- conflict
  A resource operation failed due to conflicting state
  (e.g. duplication, optimistic lock failure).

- invalid_reference
  A resource exists but refers to another resource that is missing
  or inconsistent.

The taxonomy records only the resource condition.
Whether the situation is a domain failure or a system defect is
determined by Conclusion.

----------------------------------------------------------------------
## Category: state
----------------------------------------------------------------------

The `state` category represents violations of internal invariants
or illegal states discovered after a resource has been obtained.

### state Subcategories

- illegal
  The system or entity is in a state that must not occur.

- invalid
  The state violates expected constraints or transitions.

State errors indicate internal inconsistency rather than input failure.

----------------------------------------------------------------------
## Category: system
----------------------------------------------------------------------

The `system` category represents fundamental system-level failures
not attributable to input, resource existence, or state.

Examples include unrecoverable defects or invariant violations.

----------------------------------------------------------------------
## Relationship Between Resource and I/O Errors
----------------------------------------------------------------------

I/O failures describe *access mechanisms* (network, database, file),
while resource errors describe *resource conditions*.

These concerns are orthogonal.

For example:
- A database connection failure results in `resource.unavailable`
- A missing record results in `resource.not_found`
- The I/O mechanism itself is not part of the taxonomy

----------------------------------------------------------------------
## Design Notes
----------------------------------------------------------------------

- The taxonomy intentionally avoids encoding severity or reaction
- Provenance (entity I/O, external integration, database access)
  must be recorded as contextual metadata, not taxonomy
- This taxonomy is a normalization of the legacy goldenport model,
  not a conceptual redesign

No API or specification in Phase 2.9 should contradict this taxonomy.
