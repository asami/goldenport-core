# Scala Conclusion Model — Taxonomy / Observation Design Notes
(Phase 2.9 Error Taxonomy / Observation)

status=work-in-progress
published_at=2026-01-24

----------------------------------------------------------------------
1. Purpose and Scope
----------------------------------------------------------------------
1.1 Successor Relationship to Legacy Observation Model
----------------------------------------------------------------------

This Observation model introduced in Phase 2.9 is intended as the successor to
the existing `org.goldenport.observation.Observation` model.

The legacy Observation model was designed as a general-purpose observability
representation and correctly captured telemetry concerns such as timestamp,
agent, location, tracing, and contextual metadata. However, its expressiveness
for error-related observations was limited, particularly in the ability to
represent "what is factually wrong" independently of interpretation.

Phase 2.9 strengthens the Observation model by introducing an explicit
taxonomy (Category × Symptom) that fixes error-related facts prior to any
interpretation or disposition, while preserving the core observability
requirements of the legacy model.

Once the observability requirements of the existing Observation model are fully
incorporated into the unified Phase 2.9 Observation model, the legacy
`org.goldenport.observation.Observation` is expected to be replaced by this
unified successor model.
----------------------------------------------------------------------

This document records the design decisions for the Scala-based
Conclusion / Observation model established during Phase 2.9 of the
Error Taxonomy redesign.

The primary goals of this phase are:

- To treat errors as observed facts (Observation), not judgments
- To establish a stable taxonomy vocabulary representing
  “what is factually wrong”
- To completely separate taxonomy from Interpretation, Disposition,
  and Presentation
- To use Scala implementations as a carrier of fixed specifications,
  not as a source of semantics

----------------------------------------------------------------------
2. Core Design Principle
----------------------------------------------------------------------

2.1 Observation-Centered Design

In Phase 2.9, the core problem was re-identified as Observation,
not Conclusion.

When Observation is weak:

- Interpretation becomes arbitrary
- Conclusions appear undecided or implicit
- Presentation takes precedence over meaning

Therefore, Observation is defined as:

Observation =
  Taxonomy (what is wrong)
  + provenance (where and how it was observed)

----------------------------------------------------------------------
3. Taxonomy Design
----------------------------------------------------------------------

3.1 Role of Taxonomy

Taxonomy represents only the following:

- What is factually wrong as an observed fact
- No interpretation, responsibility, severity, or handling
- A long-term stable classification vocabulary

Taxonomy is not an error code, exception type, or control-flow
mechanism. It is a classification label for observation.

----------------------------------------------------------------------
3.2 Two-Axis Model
----------------------------------------------------------------------

Taxonomy is composed of exactly two axes:

  case class Taxonomy(
    category: Taxonomy.Category,
    symptom: Taxonomy.Symptom
  )

- Category:
  The domain or area where the problem is observed

- Symptom:
  The observed manifestation of something going wrong

No additional axes (severity, retryability, responsibility, etc.)
are introduced at this level. These concerns belong to Interpretation
or Disposition.

----------------------------------------------------------------------
3.3 Category (Fixed)
----------------------------------------------------------------------

The following categories are fixed in Phase 2.9:

- argument
- property
- configuration
- resource
- state
- system

Category indicates the observational domain, not responsibility
or handling.

### 3.3.1 Category Usage Definitions

- **argument**  
  The problem is observed in input arguments explicitly provided to an operation or function.  
  This category is used when the observed issue concerns values supplied by the caller at invocation time.  
  It does not imply caller responsibility or fault.

- **property**  
  The problem is observed in properties or attributes of a domain object or entity.  
  This category is used when the issue concerns stored or derived attribute values, not direct input arguments.

- **configuration**  
  The problem is observed in runtime or environment configuration.  
  This category is used for issues related to configuration values, settings, or parameters that control system behavior.

- **resource**  
  The problem is observed in a resource or reference to a resource.  
  This category covers existence, availability, integrity, and reference-related issues of internal or external resources.

- **state**  
  The problem is observed in the state of a system, component, or domain entity.  
  This category is used when the issue concerns invalid or prohibited states or state transitions.

- **system**  
  The problem is observed at the system level and cannot be meaningfully attributed to argument, property, configuration, resource, or state.  
  This category is used as a last resort for fundamental or infrastructural failures.

----------------------------------------------------------------------
3.4 Symptom (Independent Axis)
----------------------------------------------------------------------

Symptom is defined as an independent observational axis,
orthogonal to Category.

Design principles:

- Do not enumerate all Category × Symptom combinations
- Avoid combinatorial explosion
- Allow future extensions
- Ensure symptoms overlap as little as possible in meaning

The current provisional symptom set includes:

- syntax_error
- format_error
- domain_value
- missing
- unexpected
- unsupported
- not_found
- unavailable
- conflict
- invalid_reference
- illegal
- invalid
- corrupted

These symptoms are not candidates for reduction.
Instead, their semantic boundaries are to be clarified
through specification.

### 3.4.1 Symptom Usage Definitions

- **syntax_error**  
  The observed data or structure does not conform to the required syntactic rules and cannot be parsed as intended.

- **format_error**  
  The observed data is syntactically valid but does not match the expected format or representation.

- **domain_value**  
  The observed value violates domain-level constraints or rules, despite being syntactically and structurally valid.

- **missing**  
  A required value or element is not provided or not present where it is expected to exist.

- **unexpected**  
  An observed value, element, or condition appears that is not expected in the given context.

- **unsupported**  
  The observed value, operation, or condition is explicitly not supported by the system or component.

- **not_found**  
  An attempt to locate or retrieve a target confirms that the target does not exist.

- **unavailable**  
  The target may exist, but its existence or accessibility cannot be confirmed due to reachability or availability issues.

- **conflict**  
  The observed condition represents a conflict, such as a concurrency conflict or a violation of consistency constraints.

- **invalid_reference**  
  A reference points to a target that is invalid, inconsistent, or cannot be resolved as a valid reference.

- **illegal**  
  The observed operation or state is explicitly prohibited by defined rules or constraints.

- **invalid**  
  The observed value or state does not satisfy the conditions required to be considered valid or well-formed.

- **corrupted**  
  The observed data or resource is damaged, inconsistent, or internally broken in a way that prevents correct use.

----------------------------------------------------------------------
3.5 Category × Symptom Combinations
----------------------------------------------------------------------

Design decisions:

- Any Category × Symptom combination is allowed by default
- Only combinations that are semantically impossible to observe
  are explicitly forbidden
- No constraints are enforced at the Scala type level

Rationale:

- Taxonomy is not a validation or correctness layer
- Observation records facts, even unusual ones
- Practical usage relies on higher-level DSLs

----------------------------------------------------------------------
4. DSL-Based Assumption
----------------------------------------------------------------------

In real-world usage, Taxonomy instances are not created directly.

They are generated through higher-level DSLs, such as:

  Consequence.argument.syntaxError("xxx")

As a result:

- Invalid Category × Symptom combinations are unlikely in practice
- Responsibility for correctness lies in the DSL layer
- Taxonomy acts purely as a container for DSL-generated observations

----------------------------------------------------------------------
5. The `value` Field
----------------------------------------------------------------------

Both Category and Symptom include a numeric `value` field:

  enum Category(val name: String, val value: Int)
  enum Symptom(val name: String, val value: Int)

This value:

- Is not an error code at this stage
- Does not represent severity or importance
- Exists solely as a placeholder for future stable error numbering

In Phase 2.9, no further semantics are attached to `value`.

----------------------------------------------------------------------
6. Unified Observation Structure (Authoritative)
----------------------------------------------------------------------

This section reflects the finalized Observation structure as defined in
Observation.scala and is authoritative for Phase 2.9.

Observation represents a factual record of something that was observed.
It MUST be interpretation-free, behavior-free, and normalization-free.

The unified Observation model explicitly separates:

- what was factually wrong (Taxonomy)
- how it failed (Cause)
- where the trigger originated (Source)
- how it was accessed or communicated (Channel)
- where it surfaced in the execution stack (Substrate)
- where it was observed in code (Origin)

Observability-related fields provide contextual facts only and MUST NOT
imply responsibility, handling strategy, or disposition.


----------------------------------------------------------------------
6.1 Core Observation (Required)
----------------------------------------------------------------------

taxonomy
  Category × Symptom pair representing what is factually wrong.
  This axis fixes error-related facts prior to any interpretation.

cause
  Describes how the failure occurred (failure mechanism).
  This captures the observed mode of failure, not intent or responsibility.

source
  Classifies where the triggering interaction originated.
  This axis describes the origin domain of the trigger itself
  (e.g. user input, external system, event, message),
  independent of channel, substrate, or responsibility.

channel
  Classifies how the interaction or access was performed.
  This axis describes the access or communication mechanism
  (e.g. in-process, network, messaging),
  independent of source or interpretation.

substrate
  Classifies where the condition manifested in the execution stack.
  This axis describes the runtime or technical substrate
  (e.g. JVM, OS, middleware, container) where the observation surfaced.

origin
  Represents the code location where the observation was recorded.
  This refers to the point in code where the observation was emitted
  (typically the top frame of a stack trace).
  Origin MUST NOT encode responsibility or interpretation.


----------------------------------------------------------------------
6.2 Observability Core (Required)
----------------------------------------------------------------------

timestamp
  Time at which the observation occurred.
  Required for ordering, correlation, and tracing.

subject
  Logical subject affected by the observation.
  Represents who or what is impacted, not responsibility.

agent
  Entity that performed the action or detected the phenomenon.
  Distinct from subject and not an assignment of blame.

object
  Target of the observation, such as a resource or domain entity.
  May be Unknown, but the axis must exist.


----------------------------------------------------------------------
6.3 Observability Extensions (Optional)
----------------------------------------------------------------------

traceId
  Identifier used for distributed tracing.

spanId
  Sub-identifier within a trace, when tracing is enabled.

severity
  Subjective severity recorded by the observer at observation time.
  This represents the observer’s perceived seriousness when the observation
  was taken.
  It MUST NOT be interpreted as evaluation, handling priority, or
  presentation severity.

environment
  Operational and deployment context in which the observation occurred.
  This aggregates execution mode, physical or network location,
  and execution platform.

message
  Human-readable message describing the observation.
  Intended for diagnostics and display only.

exception
  Raw exception associated with the observation, if any.
  Treated as diagnostic material only.

properties
  Arbitrary key-value metadata attached to the observation.
  Must not affect interpretation semantics.

observationId
  Unique identifier for the observation instance.
  Intended for persistence, correlation, or external reference.


----------------------------------------------------------------------
6.4 Interaction Model (Shared Component)
----------------------------------------------------------------------

Source and Channel may optionally carry structured interaction details.

Interaction represents a concrete interaction pattern such as:

- HTTP requests
- gRPC calls
- database access
- filesystem access
- messaging system access

Interaction is a shared descriptive component and does not define
semantic meaning by itself.
Semantic meaning is determined by the surrounding axis
(e.g. Source.Kind or Channel.Kind).


----------------------------------------------------------------------
Explicit Exclusions (Reconfirmed)
----------------------------------------------------------------------

The unified Observation model MUST NOT include:

- evaluated or derived severity
- retryability
- strategy
- handler
- responsibility attribution
- control-flow or exception semantics

These concerns belong to Interpretation or Disposition layers.


----------------------------------------------------------------------
Design Notes (Phase 2.9)
----------------------------------------------------------------------

- This Observation model is a successor, not a derivative, of the legacy
  org.goldenport.observation.Observation.
- Observability requirements from the legacy model are preserved,
  while error-related expressiveness is strengthened through taxonomy,
  cause, and explicit provenance axes.
- Scala implementations are treated as carriers of fixed specifications,
  not as sources of semantics.
- This document reflects the state fixed by Observation.scala and must
  be updated only when the code-level specification changes.
- Observation.severity denotes observer-recorded subjective severity at
  observation time and is distinct from any derived or presentation-level
  severity.

----------------------------------------------------------------------
7. Out of Scope for Phase 2.9
----------------------------------------------------------------------

The following are explicitly out of scope:

- Finalizing Conclusion or Disposition models
- Adding derived or presentation severity, retryability, or responsibility
- Enforcing Category × Symptom constraints in Scala types
- Prematurely encoding semantics in implementation

----------------------------------------------------------------------
8. Handover to the Next Phase
----------------------------------------------------------------------

Based on the decisions in this document, subsequent work includes:

- Writing precise semantic definitions (MUST / MUST NOT)
  for each Symptom
- Aligning DSL APIs with taxonomy generation
- Designing Interpretation and Disposition models
