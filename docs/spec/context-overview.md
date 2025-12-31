Context Overview
================

status=stable
scope=core
audience=core / CNCF / infra / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document provides a high-level overview of the Context model used
in the system, explaining how multiple independent Contexts are
structured, related, and composed at execution time.

It serves as an entry point to the detailed Context specifications.

----------------------------------------------------------------------
2. Context Philosophy
----------------------------------------------------------------------

A Context represents an explicit, immutable snapshot of assumptions
and facts required to interpret execution correctly.

Contexts are:

- Value-backed and immutable
- Explicitly constructed
- Free from I/O and side effects
- Single-purpose and non-overlapping

No Context performs detection or inference on its own.

----------------------------------------------------------------------
3. Individual Contexts
----------------------------------------------------------------------

### 3.1 EnvironmentContext

EnvironmentContext represents assumptions about the execution location
and host environment.

Typical contents include:

- Working directory
- Operating system
- Host name
- OS user

It answers the question: *where is this running?*

----------------------------------------------------------------------
### 3.2 VirtualMachineContext

VirtualMachineContext represents runtime characteristics observed by
the virtual machine.

Typical contents include:

- Clock and timezone
- Encoding and line separator
- Math and numeric settings
- Environment variables (snapshot)

It answers the question: *how is the runtime configured?*

----------------------------------------------------------------------
### 3.3 I18nContext

I18nContext represents language-related interpretation rules.

Typical contents include:

- Text normalization policy
- Text comparison policy
- Date/time formatting policy
- Locale-related configuration

It answers the question: *how should text and language-dependent values
be interpreted?*

----------------------------------------------------------------------
4. ExecutionContext
----------------------------------------------------------------------

ExecutionContext composes all individual Contexts into a single,
immutable execution-time view.

It does not introduce new semantics and does not reinterpret
sub-context contents.

ExecutionContext answers the question: *under which complete set of
assumptions is this execution taking place?*

----------------------------------------------------------------------
5. Construction and Flow
----------------------------------------------------------------------

Contexts are constructed explicitly during application bootstrap:

1. EnvironmentContext is detected
2. VirtualMachineContext is detected
3. I18nContext is constructed
4. ExecutionContext is composed

After construction, no Context is mutated.

----------------------------------------------------------------------
6. Relationship to Other Layers
----------------------------------------------------------------------

- core:
  - Defines Context structures and composition
- infrastructure / CNCF:
  - Performs detection and construction
- application / domain:
  - Consumes ExecutionContext

Contexts do not depend on application logic.

----------------------------------------------------------------------
7. Navigation
----------------------------------------------------------------------

For detailed specifications, refer to:

- environment-context.md
- virtual-machine-context.md
- i18n-context.md
- execution-context.md
- execution-context-bootstrap.md

END
----------------------------------------------------------------------
