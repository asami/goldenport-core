TODO – simplemodeling-lib
Core Observation / Conclusion / Consequence

This TODO list is derived from the handover document:

    HANDOVER – Observation / Conclusion / Consequence
    Core Library (simplemodeling-lib)

This list focuses exclusively on the Core Library.
Framework-specific concerns (CNCF, SIE, Cozy runtime) are explicitly excluded.

----------------------------------------------------------------------  
0. Guiding Principles (Read First)  
----------------------------------------------------------------------  

- Observation / Conclusion / Consequence are core language constructs
- Backward compatibility is critical
- Favor semantic clarity over convenience
- Core library must not depend on any framework or runtime
- No logging, exporting, or I/O in this project

----------------------------------------------------------------------  
0.1 Documentation and Specification Policy
----------------------------------------------------------------------  

    - Core specifications are written in RFC-style numbered sections
    - Subsections use numeric hierarchy (e.g. 1.1, 2.3)
    - Specifications are normative; notes are non-normative
    - docs/spec defines normative core specifications (including contracts that upper layers such as CNCF must respect)
    - docs/notes preserves exploratory and empirical design context
    - Applications (e.g. SIE) are validation sources, not spec authorities

----------------------------------------------------------------------  
0.2 Context Model Status
----------------------------------------------------------------------  

Completed:
    - Context spec set (EnvironmentContext / VirtualMachineContext / I18nContext / ExecutionContext)
    - ExecutionContext composition (EnvironmentContext / VirtualMachineContext / I18nContext)
    - Executable specs for Context composition (structure-only)

Next Phase:
    - Engine-side usage guidelines aligned with Context specs (no new core convenience APIs)
    - CNCF extension points for context detection and bootstrap
    - Resolver / Manager implementations (outside core)

----------------------------------------------------------------------  
1. Highest Priority Tasks (Must Do First)  
----------------------------------------------------------------------  

----------------------------------------------------------------------  
1.1 Implement Conclusion.from(Throwable)  
----------------------------------------------------------------------  

Status: NOT IMPLEMENTED

Purpose:
    - Provide a bridge for legacy or external code
    - Be used only at system boundaries

Rules:
    - Must NOT be used for normal domain failures
    - Must NOT encode domain meaning
    - Must NOT inspect framework-specific exception types

Expected behavior:
    - WebCode = InternalError (500 equivalent)
    - Observation is created with:
        - CauseKind = Exception or Defect
        - Minimal required fields only

Notes:
    - This method exists to *contain* exceptions, not encourage them
    - Design first, then implement
    - Design note MUST be written under docs/spec/ or docs/notes before implementation

----------------------------------------------------------------------  
1.2 Clarify Defect vs Domain Failure Mapping  
----------------------------------------------------------------------  

Status: PARTIALLY DEFINED

Tasks:
    - Explicitly document:
        - What is considered a defect
        - What is considered a domain failure
    - Define how each maps to:
        - Observation
        - Conclusion
        - Consequence

Deliverables:
    - Short design note (can be inline Scaladoc or docs/)
    - No framework assumptions

----------------------------------------------------------------------  
1.3 ErrorSystem / ErrorStrategy / ErrorCode (Core)
----------------------------------------------------------------------

Status: BASIC STRUCTURE IMPLEMENTED (2025-12-18)

Notes:
    - ErrorSystem trait is defined as a minimal abstraction point
    - DefaultErrorSystem (core, ultra-thin) exists as a placeholder
    - ErrorStrategy is defined as a declarative hint only
        - No execution semantics
        - No handler selection
        - No side effects
    - ErrorCode namespace is reserved under org.simplemodeling.error
        - SimpleModeling default codes are treated as a default implementation
        - Application-specific codes are expected to extend/override in upper layers

Rules:
    - Core must not interpret ErrorStrategy
    - Core must not execute retries, escalation, or handling
    - ErrorSystem exists only to *materialize* Conclusion from Observation + context

Deferred:
    - Semantic meaning of each ErrorStrategy
    - Mapping rules between ErrorCode and WebCode
    - CNCF / SIE specific ErrorSystem overrides

----------------------------------------------------------------------  
2. Observation Model Tasks  
----------------------------------------------------------------------  

----------------------------------------------------------------------  
2.x Observation Usage Scope (Config / CLI Phase)  
----------------------------------------------------------------------  

Status: EXPLICITLY DEFERRED

Notes:
    - In the current Config / CLI development phase:
        - `strategy` is NOT used
        - `handler` is NOT used
        - `code` / `DetailCode` is NOT used
    - Observation represents factual events only.
    - Interpretation (reaction, handling, classification) is delegated to:
        - ErrorSystem
        - Higher layers (CNCF, SIE, CLI)
    - strategy / handler fields are retained for future phases but intentionally unused in Config phase

Tasks (Deferred):
    - Decide whether `strategy` and `handler` should:
        - Remain in Observation as optional metadata, or
        - Be moved entirely to Conclusion / ErrorSystem
    - Define semantics of Observation.Code / DetailCode:
        - Stable classification identifier only
        - No transport (HTTP/CLI) meaning
        - No reaction or retry semantics

Non-goals (This Phase):
    - Do NOT infer retry / escalate behavior from Observation
    - Do NOT map Observation directly to Web / HTTP status codes

----------------------------------------------------------------------  
2.1 Define Observation Construction Guidelines (Documentation)  
----------------------------------------------------------------------  

Status: NOT STARTED

Tasks:
    - Define which fields are mandatory
    - Define which fields are optional
    - Provide guidance for:
        - Severity selection
        - Cause and CauseKind usage
        - Resource / Subject / Agent usage
        - SystemLocation usage
        - Strategy usage
    - Define how `previous` (causal chaining) should be used

Deliverable:
    - docs/spec/observation-guidelines.md (normative) or docs/notes/observation-guidelines.md (exploratory)

Notes:
    - This is a design document task
    - No code generation or helpers yet

----------------------------------------------------------------------  
2.2 Review Existing Observation Vocabulary  
----------------------------------------------------------------------  

Status: IN PROGRESS (Vocabulary Exists)

Tasks:
    - Review current vocabulary for:
        - Redundancy
        - Ambiguity
        - Missing core concepts
    - Ensure all concepts are:
        - Framework-agnostic
        - Transport-agnostic

Non-goals:
    - Do NOT add logging-related fields
    - Do NOT add HTTP or protocol concepts

----------------------------------------------------------------------  
3. Conclusion Model Tasks  
----------------------------------------------------------------------  

----------------------------------------------------------------------  
3.x Define Severity Policy (Conclusion Aggregation)  
----------------------------------------------------------------------  

Status: NOT STARTED

Purpose:
    - Make severity handling explicit and deterministic
    - Avoid implicit or ad-hoc interpretation in upper layers

Scope:
    - Define how severity is derived when:
        - Multiple Observations are aggregated
        - Multiple Conclusions are combined via `Conclusion.++`
    - Clarify the relationship between:
        - Observation severity
        - Conclusion status / severity-like concepts
        - ErrorSystem responsibility

Key Questions:
    - Is severity a total order (e.g. Info < Warning < Error < Fatal)?
    - Is severity represented directly, or inferred from Status / ErrorCode?
    - Should aggregation always select the maximum severity?
    - Are there contexts where severity escalation or suppression is allowed?

Rules:
    - Severity policy must be framework-agnostic
    - Severity policy must not imply execution behavior
    - Core must not perform handling, only classification

Deliverables:
    - Design note under docs/spec/ (or inline Scaladoc)
    - Minimal supporting code if required

Notes:
    - This task is intentionally separated from `Conclusion.++` implementation
    - The current implementation assumes "max severity" semantics by convention

Notes:
    - Conclusion.Status is treated as a declarative structure composed of:
        - WebCode
        - ErrorCode
        - ErrorStrategy
    - Status does not imply execution or handling behavior in core

----------------------------------------------------------------------  
3.1 Define Initial WebCode Vocabulary  
----------------------------------------------------------------------  

Status: NOT STARTED

Tasks:
    - Define minimal classification-only WebCode set
    - Candidate codes:
        - 400
        - 404
        - 409
        - 422
        - 500

Rules:
    - WebCode is classification, not explanation
    - Do NOT encode domain semantics
    - Do NOT tie directly to HTTP handling

Deliverable:
    - Code + brief Scaladoc explanation

----------------------------------------------------------------------  
3.2 Review Conclusion Structure and Fields  
----------------------------------------------------------------------  

Status: PARTIALLY IMPLEMENTED

Tasks:
    - Validate that Conclusion fields are sufficient for:
        - Diagnosis
        - Causal analysis
        - Observability correlation
    - Ensure `previous` chaining semantics are clear

----------------------------------------------------------------------  
4. Consequence Model Tasks  
----------------------------------------------------------------------  

----------------------------------------------------------------------  
4.1 Complete Missing Convenience Methods  
----------------------------------------------------------------------  

Status: PARTIALLY IMPLEMENTED

Tasks:
    - Implement takeOrMissingPropertyFault
    - Review existing constructors for:
        - Consistency
        - Naming clarity

Rules:
    - No exceptions for domain failures
    - All failures must carry Conclusion

----------------------------------------------------------------------  
4.2 Review Monad Semantics  
----------------------------------------------------------------------  

Status: IMPLEMENTED, NEEDS REVIEW

Tasks:
    - Verify monad laws are respected
    - Confirm behavior for:
        - FlatMap on Failure
        - Mapping Failure vs Success

----------------------------------------------------------------------  
5. Documentation and Alignment  
----------------------------------------------------------------------  

----------------------------------------------------------------------  
5.0 Spec / Notes / README Alignment
----------------------------------------------------------------------  

Status: UPDATED (2025-12-22)

Notes:
    - README.md provides the architectural overview
    - docs/spec/error-observation-integration.md is the normative spec
    - docs/notes/error-model-exploration.md is non-normative
    - All three documents now share a consistent role definition

Non-goals:
    - Do NOT duplicate spec content in README
    - Do NOT move exploratory discussion into spec documents

----------------------------------------------------------------------  
5.1 Align RULE.md with Error Model  
----------------------------------------------------------------------  

Status: MOSTLY ALIGNED

Tasks:
    - Ensure RULE.md explicitly states:
        - Exceptions are for defects only
        - Domain failures use Consequence
    - Add references to:
        - Observation
        - Conclusion
        - Consequence

----------------------------------------------------------------------  
5.2 Prepare OpenTelemetry Mapping Notes (Non-code)  
----------------------------------------------------------------------  

Status: NOT STARTED

Tasks:
    - Describe how Observation maps to:
        - OTel Traces
        - OTel Logs
        - OTel Metrics
    - Define attribute naming conventions
    - Define correlation strategy (traceId / spanId)

Rules:
    - Documentation only
    - No exporter or SDK dependency

----------------------------------------------------------------------  
5.3 Base DataType Profile Integration
----------------------------------------------------------------------  

Status: NOT STARTED

Purpose:
    - Align simplemodeling-lib with the published
      "Base DataType Profile" article
    - Establish a normative vocabulary of basic data types
      for domain modeling and code generation

Scope:
    - Only types explicitly marked as "SimpleModeling Lib"
      in the Base DataType Profile
    - No redefinition of Scala / Java standard types
    - No framework or runtime dependencies

Tasks:
    - Define package structure under:
        org.simplemodeling.datatype
    - Implement constrained numeric types:
        - PositiveInt, NegativeInt
        - NonNegativeInt, NonPositiveInt
        - PositiveLong, NegativeLong
        - NonNegativeLong, NonPositiveLong
    - Implement semantic text types:
        - Token, Identifier, Slug
        - PathName, FileName
        - Name, Title, Label, Text
    - Implement quantity-related types:
        - Quantity, Amount
        - Rate, Percentage
        - QuantityUnit, MeasurementUnit
    - Implement i18n-related types:
        - LocaleLanguage
        - LocaleCountry
        - LocaleScript
        - I18nString
        - I18nMessage
        - I18nTemplate
    - Implement I/O related types:
        - MimeType
        - ContentType
        - FileSuffix

Rules:
    - Constructors MUST be private
    - Public construction MUST be explicit (from / parse / unsafe)
    - Validation failures MUST NOT throw exceptions
    - No arithmetic, unit conversion, or domain logic
    - I18nContext (execution semantics) and I18n* data types (domain/value types) MUST remain decoupled

Deliverables:
    - Scala implementations
    - Scaladoc referencing the Base DataType Profile
    - Normative specification under docs/spec/base-datatype-profile.md

Non-goals:
    - Do NOT wrap Scala / Java standard library types
    - Do NOT implement XML Schema types
    - Do NOT add validation frameworks
    - Do NOT add serialization, persistence, or transport logic

----------------------------------------------------------------------  
6. Explicit Non-Goals  
----------------------------------------------------------------------  

The following must NOT be done in this project:

    - Add logging implementations
    - Add OpenTelemetry exporters
    - Add HTTP / CLI / MCP semantics
    - Add framework-specific helpers
    - Add convenience APIs that hide semantics

----------------------------------------------------------------------  
7. Final Reminder  
----------------------------------------------------------------------  

This project defines a shared language.

Once CNCF, SIE, and Cozy-generated systems depend on it,
semantic changes become extremely expensive.

Be conservative.
Be explicit.
Design first.
