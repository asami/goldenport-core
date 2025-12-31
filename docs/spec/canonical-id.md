CanonicalId
===========

status=stable
scope=core
audience=core / infrastructure / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

CanonicalId is a canonical, opaque identifier used to correlate
observations, conclusions, consequences, and executions across
system boundaries.

It exists to provide stable identity and traceability, not semantic
meaning.

----------------------------------------------------------------------
2. Core Principles
----------------------------------------------------------------------

CanonicalId MUST be:

- Opaque to program logic
- Stable as a value
- Safe to log and inspect
- Comparable by value only

CanonicalId MUST NOT:

- Encode domain or business semantics
- Be parsed or interpreted by program logic
- Be used for branching or decision making
- Depend on any framework or runtime

----------------------------------------------------------------------
3. Representation
----------------------------------------------------------------------

CanonicalId is represented as a String value.

The string representation is intended for human and AI inspection,
correlation, and observability only.

Programs MUST treat CanonicalId as an opaque value and MUST NOT
extract or interpret any internal structure.

----------------------------------------------------------------------
4. Canonical Format (Non-semantic)
----------------------------------------------------------------------

The canonical string format is fixed for interoperability and
observability, but carries no semantic meaning for programs.

Format:

    <service>-<operation>-<kind>-<timestamp>-<entropy>

Where:

- service: canonical service identifier
- operation: canonical operation identifier
- kind: usage category (e.g. exec, trace, event)
- timestamp: UTC timestamp in an ordering-friendly representation
- entropy: collision-avoidance component

Programs MUST NOT depend on, parse, or branch on any part of this
format.

----------------------------------------------------------------------
5. Generation Rules
----------------------------------------------------------------------

CanonicalId generation MUST be:

- Centralized
- Deterministic under fixed inputs
- Based on injected clock and entropy sources
- Free from global state

CanonicalId generation MUST NOT:

- Use implicit system clocks
- Use hidden randomness
- Depend on environment-specific behavior

----------------------------------------------------------------------
6. Equality and Identity
----------------------------------------------------------------------

Two CanonicalId instances are equal if and only if their string values
are equal.

No other notion of identity is defined.

----------------------------------------------------------------------
7. Layering Rules
----------------------------------------------------------------------

- core:
  - Defines CanonicalId and its generation rules
  - Treats CanonicalId as an opaque value

- upper layers (e.g. CNCF, applications):
  - MAY wrap CanonicalId into contextual identifiers
  - MUST NOT redefine CanonicalId semantics or format

----------------------------------------------------------------------
8. Explicit Non-Goals
----------------------------------------------------------------------

The following are explicitly out of scope:

- Semantic or "smart" identifiers
- Parsing or helper utilities
- Validation beyond minimal structural validity
- Mapping to transport or protocol identifiers
- Observability exporters or logging integrations

----------------------------------------------------------------------
9. Rationale
----------------------------------------------------------------------

By enforcing opacity and strict layering, CanonicalId provides stable
correlation and observability while preventing semantic leakage and
tight coupling across system layers.

END
----------------------------------------------------------------------
