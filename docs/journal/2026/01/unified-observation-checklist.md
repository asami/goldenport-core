----------------------------------------------------------------------
Unified Observation — Observability Requirements Checklist
----------------------------------------------------------------------

Purpose:
This checklist enumerates observability-related requirements that must
be incorporated into the unified Observation model, which will replace
the legacy org.goldenport.observation.Observation.

This list is used to:
- Ensure no loss of observability capability
- Separate observational facts from interpretation
- Decide which fields are mandatory, optional, or deferred


----------------------------------------------------------------------
A. Temporal and Identity Context
----------------------------------------------------------------------

[REQUIRED]

- timestamp
  - Represents when the observation occurred
  - Required for ordering, correlation, and tracing

- traceId
  - Identifier for distributed tracing
  - Optional but strongly recommended

- spanId
  - Sub-identifier within a trace
  - Optional but recommended when tracing is enabled


----------------------------------------------------------------------
B. Actor and Subject Context
----------------------------------------------------------------------

[REQUIRED]

- subject
  - Logical subject of the observation (e.g., user, system, component)
  - Represents "who is affected", not responsibility

- agent
  - Entity that performed the action or detected the phenomenon
  - Distinct from subject

[OPTIONAL]

- principal / identity
  - Authentication or authorization identity
  - Explicitly observational, not evaluative


----------------------------------------------------------------------
C. Target / Object Context
----------------------------------------------------------------------

[REQUIRED]

- object (resource / entity / target)
  - What the observation is about
  - May be Unknown, but the axis must exist

[OPTIONAL]

- objectType / objectId
  - Structured identification of the target
  - Useful for correlation and indexing


----------------------------------------------------------------------
D. Location and Execution Context
----------------------------------------------------------------------

[REQUIRED]

- location
  - Logical or physical execution location
  - Examples: node, container, process, subsystem

[OPTIONAL]

- environment
  - Environment classification (prod, staging, local, etc.)
  - Observational only


----------------------------------------------------------------------
E. Taxonomy and Error-Specific Context
----------------------------------------------------------------------

[REQUIRED]

- taxonomy (Category × Symptom)
  - What is factually wrong
  - Fixed prior to interpretation

- cause
  - How it failed (mechanism, not responsibility)
  - Optional when unknown

[EXPLICITLY EXCLUDED]

- severity
- retryability
- strategy
- handler
- responsibility


----------------------------------------------------------------------
F. Descriptive and Diagnostic Context
----------------------------------------------------------------------

[OPTIONAL]

- message
  - Human-readable description
  - I18n-capable
  - Not authoritative for semantics

- exception
  - Raw exception, if any
  - Treated as diagnostic material only

- properties
  - Arbitrary key-value metadata
  - Must not affect interpretation semantics


----------------------------------------------------------------------
G. Correlation and Classification Aids
----------------------------------------------------------------------

[OPTIONAL / DEFERRED]

- observationId
  - Unique identifier for the observation instance

- correlationId
  - Higher-level grouping identifier

- code / numeric identifier
  - Reserved for future stable numbering
  - Not used for control flow in Phase 2.9


----------------------------------------------------------------------
H. Explicit Non-Requirements
----------------------------------------------------------------------

The unified Observation model MUST NOT:

- Encode interpretation or judgment
- Encode handling or reaction strategy
- Encode severity or priority
- Enforce business rules
- Act as an exception or control-flow mechanism


----------------------------------------------------------------------
Checklist Usage
----------------------------------------------------------------------

- All REQUIRED items must be present in the unified Observation model.
- OPTIONAL items may be introduced incrementally.
- EXCLUDED items must remain outside Observation and belong to
  Interpretation or Disposition layers.

----------------------------------------------------------------------
Source — Definition and Design Rationale
----------------------------------------------------------------------

**Definition:** Source names the logical origin of the data or failure
that generated the Observation (e.g. `user_input`, `in_memory`,
`database`, `external_service`). It records *where the observed error
surface originates*, not who is affected, where it was detected, or
how the interaction crossed a boundary.

**MUST**

- describe the factual origin of the data or interaction that reported
  the problem, using the contextual terms already present in the Phase
  2.9 journals (user_input, in_memory, database, external_service, etc.).
- remain interpretation-free: do not encode responsibility, severity,
  or handling semantics into Source.
- stay stable across projections so downstream layers can refine
  responsibility, disposition, and presentation without mutating Source.

**MUST NOT**

- conflate Source with Subject—the latter records *who is affected*,
  while Source records *what data interaction caused the observation*.
- treat Source as synonymous with Origin: Origin identifies *where the
  observation was created* (method, component, macro-generated trace),
  whereas Source identifies *where the problematic data or failure
  came from*.
- substitute Source for Channel or Substrate: Channel describes the
  communication boundary over which the failure occurred, Substrate
  describes the execution layer; Source remains the data origin, not
  the invocation path or infrastructure layer.

**Distinctions**

- **Source vs Subject:** Subject is the logical entity affected by the
  observation (user, system, component). Source is the provenance of
  the observed data or failure, independent of who feels the impact.
- **Source vs Origin:** Origin records where in the codebase the
  observation was recorded (method, component, opaque origin). Source
  records where the failure data arrived from or where the faulty data
  resided before detection.
- **Source vs Channel:** Channel is about *how* the interaction happened
  (HTTP/gRPC/CLI). Source is about *where* the interaction completed
  or the data originates (database, external service, in-memory cache).
- **Source vs Substrate:** Substrate identifies the execution layer that
  manifested the failure (JVM, OS, network stack). Source identifies the
  logical source of the data that triggered the observation, regardless
  of the runtime layer.

**Rationale:** Source exists as a separate axis because understanding
the provenance of the data/failure is orthogonal to understanding who
is affected, where it was detected, and how it propagated. By keeping
Source distinct, downstream Evaluation layers can determine whether the
failure came from user input, configuration, or an external system
without conflating that insight with responsibility or execution
boundaries.


----------------------------------------------------------------------
Legacy Observation Field Mapping
----------------------------------------------------------------------

----------------------------------------------------------------------
A. Fields Carried Over Directly
----------------------------------------------------------------------

- timestamp (REQUIRED)
- traceId (OPTIONAL)
- spanId (OPTIONAL)
- subject (REQUIRED)
- agent (REQUIRED)
- object (REQUIRED)
- location (REQUIRED)
- environment (OPTIONAL)
- message (OPTIONAL)
- exception (OPTIONAL)
- properties (OPTIONAL)
- observationId (OPTIONAL / DEFERRED)

----------------------------------------------------------------------
B. Fields Absorbed by Phase 2.9 Core Axes
----------------------------------------------------------------------

- phenomenon → taxonomy
- causeKind → Cause.Kind
- cause → Cause.Detail
- descriptor → Cause.Detail or properties
- handler → Origin or Disposition layer
- strategy → Disposition layer
- code / detailCode → deferred or future concern

----------------------------------------------------------------------
C. Fields Reclassified or Moved
----------------------------------------------------------------------

- location → split across source / substrate / location
- agent → retained as agent, logical origin represented by source
- exception → retained as diagnostic, detection point represented by origin
