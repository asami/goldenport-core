ExecutionContext Design Rationale
=================================

This document consolidates the design decisions and rationale around
ExecutionContext, logging, and runtime context handling in goldenport core
and CNCF.

It is intended to serve as a stable reference for:
- core design decisions
- CNCF integration
- testability and configurability
- future contributors

----------------------------------------------------------------------
1. Background and Goals
----------------------------------------------------------------------

goldenport core is designed to be:

- usable standalone (CLI, tests) without CNCF
- testable with full control over execution assumptions
- extensible without ad-hoc access to JVM / OS defaults
- compatible with CNCF for production-grade execution

To achieve this, execution-related information must be:
- explicit
- modeled
- injectable
- testable

This led to the introduction of ExecutionContext as a core concept.

----------------------------------------------------------------------
2. ExecutionContext (goldenport core)
----------------------------------------------------------------------

ExecutionContext is the **central object of goldenport core**.

It represents the explicit execution assumptions required for core to
produce meaningful and testable results.

ExecutionContext is:

- immutable
- explicit
- injected
- independent of CNCF
- independent of runtime state

ExecutionContext is NOT:
- a runtime state container
- a lifecycle manager
- a concurrency abstraction

----------------------------------------------------------------------
3. What ExecutionContext Contains
----------------------------------------------------------------------

ExecutionContext models all execution assumptions that core code
is allowed to depend on.

These typically include:

- environment (dev / test / prod)
- channel (cli / rest / mcp)
- execution mode (interactive / batch)
- policy flags

- locale (for i18n)
- timezone
- encoding
- datetime format
- message resolver

- clock / time source
- math context (precision, rounding)
- random sequence (deterministic, testable)

- logger (logging capability)

All of these are:
- configuration-derived
- execution-preconditions
- required for reproducibility and testability

Direct access to JVM defaults such as:
- Locale.getDefault
- Instant.now
- scala.util.Random
- MathContext defaults
is prohibited in core.

----------------------------------------------------------------------
4. Logging Policy
----------------------------------------------------------------------

goldenport core logs events but does not manage logging infrastructure.

Logging is treated as a **capability** provided via ExecutionContext.

Design rules:

- Core code MUST log only via ExecutionContext.logger
- Core code MUST NOT access logging frameworks directly
- Logger is an interface defined by goldenport

Implementation strategy:

- core uses SLF4J-based adapters
- CNCF provides OpenTelemetry-based adapters
- tests provide in-memory or recording loggers

This allows:
- lightweight CLI usage
- rich observability in CNCF
- full testability

----------------------------------------------------------------------
5. RuntimeContext (CNCF)
----------------------------------------------------------------------

RuntimeContext is a CNCF concept.

RuntimeContext represents execution-time state required to
run an operation safely and controllably.

Typical RuntimeContext contents include:

- execution state (running, retrying, cancelled)
- retry counts and deadlines
- resource handles (transactions, connections)
- concurrency primitives (threads, executors)
- observability scopes (trace spans, metrics)

RuntimeContext is:

- mutable
- time-dependent
- runtime-specific
- CNCF-internal

RuntimeContext MUST NOT be exposed to goldenport core.

----------------------------------------------------------------------
6. Relationship Between ExecutionContext and RuntimeContext
----------------------------------------------------------------------

ExecutionContext and RuntimeContext have different roles:

ExecutionContext:
- describes execution assumptions
- is immutable
- is used for semantic decisions and output
- is test-friendly

RuntimeContext:
- manages execution state
- is mutable
- controls execution flow
- is infrastructure-specific

The correct relationship is:

  RuntimeContext  --(projection)-->  ExecutionContext  --> core logic

CNCF is responsible for extracting or constructing ExecutionContext
from its RuntimeContext and configuration.

----------------------------------------------------------------------
7. CNCF ExecutionContext
----------------------------------------------------------------------

CNCF defines its own ExecutionContext that extends core ExecutionContext.

Structure:

- org.goldenport.context.ExecutionContext        (core)
- org.goldenport.cncf.context.ExecutionContext   (CNCF)
    extends org.goldenport.context.ExecutionContext

CNCF ExecutionContext may include:
- RuntimeContext
- CNCF-specific execution facilities

goldenport core only depends on the core ExecutionContext.

----------------------------------------------------------------------
8. Naming Policy
----------------------------------------------------------------------

ExecutionContext is a core concept and is intentionally named plainly.

- org.goldenport.context.ExecutionContext is the canonical ExecutionContext
- scala.concurrent.ExecutionContext must be imported with an alias
  (e.g. ScalaExecutionContext)

This reflects conceptual priority:
goldenport ExecutionContext is semantic and foundational,
Scala ExecutionContext is a technical utility.

----------------------------------------------------------------------
9. Testability and Reproducibility
----------------------------------------------------------------------

By modeling execution assumptions explicitly in ExecutionContext:

- year/month boundary tests become trivial
- fiscal year transitions are reproducible
- random behavior is deterministic
- numeric precision is controlled
- multilingual output is testable
- logging behavior can be verified

ExecutionContext is therefore a first-class testing construct.

----------------------------------------------------------------------
Equality Semantics
----------------------------------------------------------------------

ExecutionContext is not a value object.

Its equality is intentionally identity-based (reference equality),
and core code MUST NOT rely on value-based equality or hashing of
ExecutionContext instances.

ExecutionContext represents an execution-scoped semantic environment,
where identity (which execution this context belongs to) matters more
than structural equality.

When comparison of execution assumptions is required (e.g. for caching,
diffing, or diagnostics), the comparison MUST be performed explicitly on:

  - ExecutionContext.Core

ExecutionContext.Core is the canonical value object that defines
structural equality of execution assumptions.

This separation allows:
- safe extension of ExecutionContext (e.g. CNCF runtime contexts)
- preservation of runtime identity
- avoidance of accidental misuse of context objects as map/set keys

----------------------------------------------------------------------
ExecutionContext.Instant
----------------------------------------------------------------------

ExecutionContext.Instant represents a concrete, execution-scoped binding
of ExecutionContext.Core.

While ExecutionContext.Core defines the canonical, reusable value of
execution assumptions, ExecutionContext.Instant represents the *actual
context instance* used for a specific execution.

Key characteristics:

- ExecutionContext.Core
  - pure value object
  - reusable and comparable
  - represents execution assumptions abstractly

- ExecutionContext.Instant
  - execution-scoped wrapper
  - identity-based (reference equality)
  - may be extended or decorated by higher layers (e.g. CNCF)

ExecutionContext.Instant exists to make the following distinction explicit:

- *what* the execution assumptions are (Core)
- *where and when* they are bound and used (Instant)

This separation allows:

- safe reuse of Core values across executions
- clear execution identity boundaries
- attachment of runtime-adjacent metadata without polluting Core
- future extension in CNCF without weakening core semantics

ExecutionContext.Instant SHOULD be the default concrete ExecutionContext
used when invoking core logic.

----------------------------------------------------------------------
ExecutionContext Creation and Lifecycle
----------------------------------------------------------------------

ExecutionContext creation and lifecycle follow simple, explicit rules
to preserve clarity, testability, and semantic correctness.

Creation Rules:

- ExecutionContext.Core SHOULD be constructed from configuration,
  defaults, or test fixtures.
- ExecutionContext.Core MUST be treated as a reusable value object.
- ExecutionContext.Instant MUST be created by binding a Core to
  a specific execution.

Typical pattern:

  core   = ExecutionContext.Core(...)
  ctx    = ExecutionContext.Instant(core)

Lifecycle Rules:

- ExecutionContext.Instant is execution-scoped.
- A single ExecutionContext.Instant SHOULD be reused consistently
  within the same execution (request, command, or operation).
- ExecutionContext.Instant MUST NOT be shared across independent
  executions.

Extension Rules:

- Higher layers (e.g. CNCF) MAY wrap or extend ExecutionContext.Instant
  to attach runtime-specific state.
- Such extensions MUST preserve the Core value unchanged.

These rules ensure that:
- execution assumptions remain explicit and reusable
- execution identity boundaries remain clear
- test fixtures remain simple and deterministic

----------------------------------------------------------------------
10. Summary
----------------------------------------------------------------------

ExecutionContext is the semantic and operational foundation of goldenport core.

- It centralizes all execution assumptions
- It enables standalone usage and testing
- It decouples core from CNCF runtime concerns
- It provides a clean integration point for logging and observability

RuntimeContext complements ExecutionContext but does not replace it.

This separation is fundamental to maintaining clarity, extensibility,
and long-term maintainability of the goldenport ecosystem.
