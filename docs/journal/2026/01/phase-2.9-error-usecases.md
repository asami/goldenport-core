# Phase 2.9 — Core Error Model Use Cases

status = draft
phase = 2.9
kind = journal

----------------------------------------------------------------------
## Purpose
----------------------------------------------------------------------

This document captures the concrete use cases that the core error model
of simplemodeling-lib is expected to satisfy.

It serves as the grounding reference for Phase 2.9 and must be fixed
before any specification or design changes are made.

**Authoritative References:** Phase 2.9 Observation and Conclusion semantics are finalized in
`docs/spec/observation-phase-2.9-authoritative.md` and
`docs/spec/conclusion-phase-2.9-authoritative.md`. Consider those documents the definitive source of
truth; this journal supplies use-case context without overriding them.

This document is intentionally written as a journal artifact:
it records intent and rationale, not normative rules.

----------------------------------------------------------------------
## Background
----------------------------------------------------------------------

Through the development of CNCF, CLI execution paths, and bootstrap-time
initialization, several mismatches and ambiguities in the current error
model have been observed.

In particular:
- Error semantics have been interpreted differently across layers
- Domain failures and system defects have occasionally been conflated
- Bootstrap and pre-runtime failures were hard to diagnose safely

These observations motivate a re-alignment of the core error model.

----------------------------------------------------------------------
## Use Case UC-01: Validation and Resolution Failures as Values
----------------------------------------------------------------------

### Scenario

During parameter resolution, configuration loading, or model validation,
multiple errors may occur before execution can proceed.

### Expectations

- Failures must be represented as values, not thrown exceptions
- Multiple failures must be aggregatable
- The caller must be able to inspect, report, or recover from failures

### Implication for Core

- Consequence represents success or failure explicitly
- Failure contains structured information, not just messages
- Control flow must not rely on exceptions

----------------------------------------------------------------------
## Use Case UC-02: Domain Failure vs System Defect Separation
----------------------------------------------------------------------

### Scenario

Some failures are caused by invalid domain input or configuration,
while others indicate defects in the system or environment.

### Expectations

- Domain failures and defects must be distinguishable by structure
- Defects may justify aborting execution
- Domain failures must remain explainable to the caller

### Implication for Core

- Observation captures factual failure causes
- Conclusion classifies the failure meaning
- Exceptions are treated as defect signals, not domain outcomes

----------------------------------------------------------------------
## Use Case UC-03: Context-Dependent Interpretation
----------------------------------------------------------------------

### Scenario

The same factual error (e.g., a missing property) may occur in different
contexts such as CLI execution, configuration loading, or runtime startup.

### Expectations

- Observations are reusable across contexts
- Interpretation depends on context, not on the observation itself
- Core must not embed CLI, HTTP, or runtime-specific semantics

### Implication for Core

- Observation is judgment-free
- Conclusion and higher layers apply interpretation
- Mapping to presentation or protocol happens outside core

----------------------------------------------------------------------
## Use Case UC-04: Bootstrap and Initialization Failures
----------------------------------------------------------------------

### Scenario

Errors occur during bootstrap phases such as component discovery,
class loading, or initial model setup.

### Expectations

- Failures must not be silently lost
- Diagnostics must remain available even if runtime initialization fails
- The system must explain why startup failed

### Implication for Core

- Throwable-to-Conclusion conversion at boundaries
- Failure information preserved as values
- No dependency on logging or runtime availability

----------------------------------------------------------------------
## Use Case UC-05: Safe Projection to External Interfaces
----------------------------------------------------------------------

### Scenario

Core errors must be presented via CLI output, logs, APIs, or diagnostic tools.

### Expectations

- Core provides stable, structured metadata
- Presentation format and wording are decided by projections
- No string-based contracts in core

### Implication for Core

- Stable identifiers (status, categories)
- Structured observations and conclusions
- Projection-specific adapters outside core

----------------------------------------------------------------------
## Use Case UC-06: Interoperability with Exception-Based Code
----------------------------------------------------------------------

### Scenario

Core code must interoperate with libraries that throw exceptions
(IO, reflection, parsing, etc.).

### Expectations

- Exceptions must not leak into domain logic
- Diagnostic information must be preserved
- System defects must remain identifiable

### Implication for Core

- Explicit exception boundary APIs
- Throwable converted into Observation / Conclusion
- Defect semantics preserved without stack-trace dependence

----------------------------------------------------------------------
## Use Case UC-07: Convenient Programmatic Error Construction

### Scenario

Core and application code frequently needs to construct common error cases
(e.g. illegal arguments, invalid state) without boilerplate or ad-hoc helpers.

### Expectations

- Errors can be constructed with concise, intention-revealing APIs
- Common cases do not require manual Conclusion/Observation wiring
- The resulting errors remain fully structured and inspectable

### Implication for Core

- Provide factory-style helpers such as:
  - Consequence.illegalArgument(message)
  - Consequence.invalidState(message)
- These helpers must produce standard Observation + Conclusion structures
- Convenience must not bypass the core semantics or vocabulary


----------------------------------------------------------------------
## Use Case UC-08: Error Vocabulary Stabilization
----------------------------------------------------------------------

### Scenario

Different parts of the system currently describe similar errors using
inconsistent wording and categories.

### Expectations

- A shared, stable vocabulary for common error kinds exists
- Vocabulary is reusable across components and applications
- Error names are more important than message strings

### Implication for Core

- Define a canonical set of error kinds / categories
- Vocabulary lives at the core level, not in projections
- Messages remain secondary and replaceable


----------------------------------------------------------------------
## Use Case UC-09: Error Chaining and Causality
----------------------------------------------------------------------

### Scenario

An error occurs as a consequence of another error (e.g. validation failure
caused by a missing configuration, which itself was caused by I/O failure).

### Expectations

- Errors can reference prior errors without losing structure
- Causal chains are preserved for diagnostics
- Higher layers can choose how much of the chain to expose

### Implication for Core

- Support explicit error chaining / causality
- Preserve original Observation(s) as part of the failure
- Avoid reliance on exception stack traces for causality


----------------------------------------------------------------------
## Use Case UC-10: Component-Level Error Translation
----------------------------------------------------------------------

### Scenario

A component operation fails internally with a low-level error
(e.g. IllegalArgument), but must return an error expressed in the
component’s own domain language.

### Expectations

- Internal errors can be translated at component boundaries
- External callers only see component-level error semantics
- Internal details are not leaked unintentionally

### Implication for Core

- Allow Conclusions to be reclassified at boundaries
- Support mapping rules such as:
  - IllegalArgument → Component.InternalError
- Translation is explicit and intentional, not implicit


----------------------------------------------------------------------
## Use Case UC-11: Structured Error Codes
----------------------------------------------------------------------

### Scenario

Applications and integrations require stable, machine-readable error codes,
sometimes including numeric sub-codes.

### Expectations

- Error codes follow consistent construction rules
- Core defines the mechanism, not application-specific values
- Codes can be extended by applications safely

### Implication for Core

- Define rules for composing detailed error codes
- Separate core-level codes from application-level extensions
- Ensure codes are stable identifiers, not presentation artifacts


----------------------------------------------------------------------
## Use Case UC-12: Error Handling Hints
----------------------------------------------------------------------

### Scenario

Callers (human or automated) need guidance on how to react to an error
(e.g. retry, fix input, contact operator).

### Expectations

- Errors can carry optional handling hints
- Hints do not prescribe behavior, only suggest
- Core defines the structure, not the policy

### Implication for Core

- Allow Conclusion to carry handling-hint metadata
- Keep hints declarative and optional
- Leave enforcement to higher layers


----------------------------------------------------------------------
## Use Case UC-13: Application-Specific Error Codes
----------------------------------------------------------------------

### Scenario

An application built on the core defines its own domain-specific error codes
and meanings.

### Expectations

- Applications can extend the error code space
- Extensions do not collide with core semantics
- Core tooling can still reason about the error structurally

### Implication for Core

- Reserve extension points for application-defined codes
- Clearly separate core vs application namespaces
- Preserve interoperability with core abstractions


----------------------------------------------------------------------
## Use Case UC-14: Mapping from Existing Exception Taxonomies
----------------------------------------------------------------------

### Scenario

Existing codebases rely on exception hierarchies and must interoperate
with the core error model during migration.

### Expectations

- Well-defined mapping rules from common exceptions
- No loss of essential diagnostic information
- Gradual migration without full rewrites

### Implication for Core

- Define canonical mappings from standard exceptions
- Make mappings explicit and documented
- Treat legacy exceptions as an integration concern, not a core leak

----------------------------------------------------------------------
## Non-Goals
----------------------------------------------------------------------

This document does NOT define:

- HTTP status mappings
- CLI output formats
- Logging backend behavior
- Framework-specific handling strategies

These are explicitly delegated to higher layers.

----------------------------------------------------------------------
## Role in Phase 2.9
----------------------------------------------------------------------

All specification and design work in Phase 2.9 must be justified
by reference to one or more of the use cases defined in this document.

Any proposed change that cannot be traced back to these use cases
is considered out of scope for Phase 2.9.
