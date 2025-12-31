ExecutionContext
================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

ExecutionContext represents an explicit, immutable snapshot of all
contextual information required to interpret and execute operations
in a well-defined manner.

It composes multiple independent contexts into a single, coherent
execution-time view without introducing additional semantics.

----------------------------------------------------------------------
2. Design Principles
----------------------------------------------------------------------

ExecutionContext MUST be:

- Explicit and value-backed
- Immutable after construction
- Free from I/O and side effects
- Declarative rather than procedural
- Suitable for logging, tracing, and AI-assisted inspection

ExecutionContext MUST NOT:

- Infer missing information implicitly
- Perform environment detection
- Contain mutable runtime state

----------------------------------------------------------------------
3. Core Composition
----------------------------------------------------------------------

ExecutionContext is composed of the following independent contexts:

- EnvironmentContext
- VirtualMachineContext
- I18nContext

Each sub-context retains its own responsibility and semantics.

ExecutionContext does not merge, override, or reinterpret the contents
of its sub-contexts.

ExecutionContext Core also carries explicit execution attributes:

- locale
- timezone
- encoding
- clock
- math
- random
- logger

----------------------------------------------------------------------
4. Responsibilities
----------------------------------------------------------------------

4.1 Context Aggregation

ExecutionContext provides a single access point to all execution
contexts required by core logic and higher layers.

It does not add new semantic meaning to the aggregated contexts.

---------------------------------------------------------------------
4.2 Language-Sensitive Operations
----------------------------------------------------------------------

ExecutionContext does not define language-sensitive convenience
operations in core.

----------------------------------------------------------------------
4.3 Resolver Access
----------------------------------------------------------------------

Resolver access is not defined in core.
Higher layers MAY extend ExecutionContext to include resolver
components without altering core semantics.

----------------------------------------------------------------------
5. Explicit Non-Goals
----------------------------------------------------------------------

ExecutionContext does NOT include:

- Business or domain state
- Application configuration logic
- I18n resource resolution
- Observability backends or exporters
- Runtime mutation or lifecycle management

----------------------------------------------------------------------
6. Relationship with Other Layers
----------------------------------------------------------------------

- core:
  - Defines the structure and composition of ExecutionContext
  - Treats ExecutionContext as an immutable value

- CNCF / infrastructure:
  - Constructs ExecutionContext during bootstrap
  - Injects concrete resolver implementations

- application / domain:
  - Consumes ExecutionContext
  - Must not modify it

----------------------------------------------------------------------
7. Rationale
----------------------------------------------------------------------

By explicitly composing independent contexts into a single immutable
structure, ExecutionContext enables reproducible execution, precise
diagnostics, and AI-assisted reasoning, while preserving strict
separation of concerns and avoiding implicit coupling.

END
----------------------------------------------------------------------
