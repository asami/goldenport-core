----------------------------------------------------------------------
Error Information Model — Observation / Interpretation / Disposition
----------------------------------------------------------------------

This document describes the complete error information model carried
by `Conclusion` in the Goldenport / SimpleModeling core.

The model separates factual observation from semantic interpretation
and actionable disposition, while allowing all information to be
physically propagated through the program as a single value.

----------------------------------------------------------------------
Overview
----------------------------------------------------------------------

Error handling information is structured into three conceptual layers:

- Observation     : factual description of what happened
- Interpretation  : semantic evaluation of the observation
- Disposition     : guidance for reaction and escalation

These layers are intentionally separated to avoid semantic drift,
while `Conclusion` serves as the transport object that carries them
together across program boundaries.

----------------------------------------------------------------------
Observation
----------------------------------------------------------------------

Observation represents *facts only*.
It records what occurred, without judgment, responsibility, or
recovery semantics.

Observation consists of the following elements:

- taxonomy
    The factual classification of the error
    (e.g. argument.missing, resource.not_found).

- cause
    The immediate triggering action or operation
    (e.g. lookup, parse, validation, execution).

- source
    The origin of the data or failure
    (e.g. user_input, in_memory, database, external_service).

- scope
    The structural scope in which the observation occurred
    (e.g. argument, property, resource, system).

Observation must remain projection-independent and stable.
No user guidance or recovery meaning is encoded here.

----------------------------------------------------------------------
Interpretation
----------------------------------------------------------------------

Interpretation evaluates an Observation within program context.

It answers questions such as:
“Is this acceptable?”
“Who is responsible?”
“Is this expected or a defect?”

Interpretation consists of:

- kind
    One of:
    - domain_failure
    - defect

- expectation
    One of:
    - allowed
    - invariant_violation

- responsibility
    Who is responsible for addressing the issue:
    - end_user
    - application_admin
    - system_admin
    - developer

Interpretation may differ depending on where and how the error is
observed, even if the Observation is identical.

----------------------------------------------------------------------
Disposition
----------------------------------------------------------------------

Disposition provides *actionable guidance* for error handling.

It is intended for projections (CLI, API, UI) and operational policy,
but is carried in `Conclusion` to ensure reliable propagation.

Disposition consists of:

- user_action
    Suggested action for the end user:
    - fix_input
    - retry_immediately
    - retry_later
    - escalate

- escalation_to
    If escalation is required, the target role:
    - application_admin
    - system_admin
    - developer

Disposition represents a *hint*, not a mandatory command.
Final decisions may still be overridden at projection time.

----------------------------------------------------------------------
Relationship to Conclusion
----------------------------------------------------------------------

`Conclusion` is the physical carrier of:

- Observation (factual core)
- Interpretation (semantic evaluation)
- Disposition (reaction guidance)

This design allows:
- error chains to refine interpretation and disposition
- higher layers to override or augment guidance
- reliable transport without out-of-band side channels

Observation MUST NOT be mutated.
Interpretation and Disposition MAY be refined through chaining.

----------------------------------------------------------------------
Design Intent
----------------------------------------------------------------------

- Observation answers: “What happened?”
- Interpretation answers: “What does it mean?”
- Disposition answers: “What should be done?”

Keeping these concerns explicit prevents accidental coupling between
error facts, semantics, and reactions, while preserving practical
usability in real systems.

----------------------------------------------------------------------
Conclusion Evaluation and Resolution Mechanism
----------------------------------------------------------------------

This section supplements the error information model by describing
how `Conclusion` is interpreted and evaluated by external interfaces
and runtime projections.

The purpose is to clarify where and how final handling decisions are
determined, without overloading the core error model.

----------------------------------------------------------------------
Role of Conclusion in the Runtime
----------------------------------------------------------------------

`Conclusion` is designed to be a *transportable semantic package*.

It does not execute reactions by itself.
Instead, it carries sufficient information so that downstream
components can deterministically evaluate and resolve it.

The core guarantees:
- Stable factual information (Observation)
- Explicit semantic interpretation (Interpretation)
- Pre-filled handling hints (Disposition)

----------------------------------------------------------------------
Evaluation Phase
----------------------------------------------------------------------

Evaluation is the process by which a consumer of `Conclusion`
determines the concrete handling strategy.

Typical evaluators include:
- CLI frontends
- HTTP / API adapters
- Batch runtimes
- Monitoring / alerting systems

Evaluation may consider:
- Conclusion.Interpretation
- Conclusion.Disposition
- Runtime policy
- Execution environment
- Deployment configuration

Evaluation MUST NOT alter Observation.
It MAY refine or override Interpretation and Disposition.

----------------------------------------------------------------------
Resolution and Final Decision
----------------------------------------------------------------------

Resolution is the act of committing to a concrete behavior, such as:

- Displaying a user-facing error message
- Selecting an HTTP status code
- Triggering retry logic
- Escalating to operators or developers
- Emitting alerts or metrics

Resolution is projection-specific and context-dependent.

`Conclusion` provides guidance, not commands.
Final responsibility lies with the evaluating layer.

----------------------------------------------------------------------
Layered Responsibility Model
----------------------------------------------------------------------

Responsibility is intentionally layered:

- Core
    Defines Observation, Interpretation, and default Disposition.

- Conclusion Chain
    Allows upstream layers to refine meaning and guidance.

- Evaluation Layer
    Applies environment-specific policy.

- Execution Layer
    Performs concrete actions.

This separation prevents:
- Hard-coded runtime behavior in core logic
- Loss of semantic intent during propagation
- Ad-hoc error handling scattered across code

----------------------------------------------------------------------
Overriding and Refinement Rules
----------------------------------------------------------------------

- Observation is immutable.
- Interpretation may be refined when new context is introduced.
- Disposition may be overridden by:
    - stricter policy
    - security constraints
    - operational configuration

Refinement must be explicit and traceable through Conclusion chaining.

----------------------------------------------------------------------
Design Rationale
----------------------------------------------------------------------

Carrying Interpretation and Disposition inside `Conclusion` ensures:

- No loss of intent across abstraction boundaries
- Deterministic evaluation in projections
- Reduced duplication of error handling logic
- Clear auditability of decision paths

The model balances semantic richness with operational flexibility,
making `Conclusion` a stable contract between core logic and runtime
behavior.

## Observation Axes — Channel and Substrate

In addition to Taxonomy, Cause, Source, and Scope, Observation records
two execution-boundary related axes: Channel and Substrate.

These axes are strictly descriptive and do not imply responsibility,
severity, or handling strategy.

### Channel

Channel represents the logical interaction boundary through which the
failure was observed.

It answers the question:
"Across which communication or invocation boundary did this failure surface?"

Typical channel values include:

- http
- grpc
- akka
- cli
- jdbc
- filesystem
- in_memory

Channel is concerned with *how the interaction was performed*, not with
the underlying execution environment.

For example:
- A REST API call failing with HTTP 500 → channel = http
- A gRPC method timing out → channel = grpc
- An actor message not delivered → channel = akka

### Substrate

Substrate represents the concrete execution or infrastructure layer
where the failure manifested.

It answers the question:
"On which execution substrate did the failure occur?"

Typical substrate values include:

- os
- jvm
- jdbc
- akka_cluster
- container
- filesystem
- network_stack

Substrate intentionally replaces the ambiguous notion of "middleware".
Operating systems, runtimes, drivers, and cluster frameworks are all
treated uniformly as execution substrates.

### Relationship Between Channel and Substrate

Channel and Substrate are orthogonal:

- Channel describes the *interaction boundary*
- Substrate describes the *execution layer*

A single Observation may involve both.

Example:

- A gRPC request fails due to a JDBC connection error:
  - channel: grpc
  - substrate: jdbc

This separation allows precise diagnosis without conflating protocol,
runtime, and resource semantics.

### Design Rationale

Separating Channel and Substrate prevents common classification errors,
such as:

- Mixing protocol details into Cause
- Treating OS-level failures as domain errors
- Losing visibility into where a failure actually surfaced

Observation remains a factual record.
Interpretation and Disposition are applied later, based on these facts.

