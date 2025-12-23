SimpleModeling Error / Observation Integration Specification
============================================================

This document defines the external integration contract
between the SimpleModeling core library and framework-level
runtime systems such as Cloud-Native Component Framework (CNCF)
and Semantic Integration Engine (SIE).

This document is normative for framework integration.
Normative rules are ultimately defined by code and RULE.md.

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This specification is for maintainers and contributors of
frameworks that integrate with the SimpleModeling core library.

It defines:
    - What the SimpleModeling core provides
    - What frameworks (e.g. CNCF) are expected to implement
    - What frameworks must NOT do
    - Where interpretation and behavior belong

The goal is to preserve a shared, stable error / observation
language across all runtimes.


----------------------------------------------------------------------
2. Core Model Summary (What Frameworks Depend On)
----------------------------------------------------------------------

The SimpleModeling core defines three fundamental concepts:

    - Observation
    - Conclusion
    - Consequence

These concepts are:
    - Framework-agnostic
    - Transport-agnostic
    - Intended to be long-term stable


----------------------------------------------------------------------
2.1 Observation (Fact)
----------------------------------------------------------------------

Observation represents a *factual event*.

Properties:
    - Describes what happened, not what to do
    - Structured and machine-readable
    - Compatible with observability systems (e.g. OpenTelemetry)
    - Contains NO execution or reaction semantics

Observation does NOT decide:
    - retry
    - escalation
    - handler selection
    - HTTP / CLI behavior

Observation is purely descriptive.


----------------------------------------------------------------------
2.2 Conclusion (Judgment)
----------------------------------------------------------------------

Conclusion represents a *judgment* made about an Observation.

Conclusion consists of:
    - Status (classification only)
    - Observation
    - Optional causal chain (previous)

Conclusion.Status is a declarative structure composed of:
    - WebCode       (classification, not transport)
    - ErrorCode     (SimpleModeling / application-specific)
    - ErrorStrategy (hint only, no execution semantics)

Conclusion does NOT execute behavior.
It represents interpretation, not action.


----------------------------------------------------------------------
2.3 Consequence[T] (Outcome)
----------------------------------------------------------------------

Consequence[T] represents the outcome of an operation.

Forms:
    - Success(result, conclusion)
    - Failure(conclusion)

Rules:
    - No exceptions for domain failures
    - All failures must carry a Conclusion
    - Exceptions are reserved for defects only

Consequence is the primary error propagation mechanism
used by frameworks such as CNCF.


----------------------------------------------------------------------
3. ErrorSystem (Framework Extension Point)
----------------------------------------------------------------------

The core library defines:

    trait ErrorSystem

This is the *only intended extension point* for
framework-specific error interpretation.


----------------------------------------------------------------------
3.1 Responsibility Split
----------------------------------------------------------------------

Core library responsibilities:
    - Define structure
    - Preserve semantics
    - Avoid interpretation and execution
    - Remain framework-independent

Framework responsibilities (e.g. CNCF):
    - Interpret Conclusion.Status
    - Map ErrorStrategy to actual behavior
    - Decide retry / escalation / abort
    - Integrate with logging, metrics, and observability


----------------------------------------------------------------------
3.2 DefaultErrorSystem (Core)
----------------------------------------------------------------------

The core library provides:

    - DefaultErrorSystem (ultra-thin)

Purpose:
    - Enable core-only usage
    - Provide safe defaults
    - Avoid nulls or missing wiring

Frameworks MUST provide their own ErrorSystem
and must NOT rely on DefaultErrorSystem for production behavior.


----------------------------------------------------------------------
4. ErrorStrategy (Declarative Hint)
----------------------------------------------------------------------

ErrorStrategy is defined as a *declarative hint*.

Examples (non-exhaustive):
    - Input
    - Retry
    - Escalate
    - None

Rules:
    - Core never executes strategies
    - Strategy has no side effects
    - Strategy is NOT a command

Framework interpretation examples:
    Retry     -> retry job or operation
    Escalate -> fail fast and emit alert
    Input    -> client-side correction required


----------------------------------------------------------------------
5. ErrorCode (Classification)
----------------------------------------------------------------------

ErrorCode resides under:

    org.simplemodeling.error

Core provides:
    - Namespace
    - Minimal default codes (e.g. SmErrorCode)

Framework responsibilities:
    - Define framework-specific ErrorCodes
    - Optionally map them to metrics or alerts

Rules:
    - ErrorCode is classification, not message
    - Do NOT embed UI or HTTP semantics
    - Do NOT encode retry logic


----------------------------------------------------------------------
6. What Frameworks Must NOT Do
----------------------------------------------------------------------

Frameworks must NOT:

    - Redefine Observation vocabulary
    - Introduce ad-hoc error models
    - Throw exceptions for expected failures
    - Bypass Conclusion or Consequence
    - Inject framework semantics into core models

All framework-specific behavior must reside in:
    - Framework ErrorSystem
    - Framework execution / runtime layers


----------------------------------------------------------------------
7. Recommended Framework Integration Steps
----------------------------------------------------------------------

Step 1:
    Implement a framework-specific ErrorSystem

Step 2:
    Define framework-specific ErrorCodes
        (e.g. JobExecutionFailure, DependencyUnavailable)

Step 3:
    Map ErrorStrategy to execution behavior
        - retry policy
        - escalation policy
        - state transitions

Step 4:
    Project Observation into observability systems
        - OpenTelemetry Traces
        - OpenTelemetry Logs
        - Metrics

(No core library changes required)


----------------------------------------------------------------------
8. Mental Model
----------------------------------------------------------------------

Core models are a *shared language*, not a framework.

Frameworks:
    - Speak the language
    - Interpret it
    - Act on it

Core:
    - Defines vocabulary
    - Preserves meaning
    - Never acts


----------------------------------------------------------------------
9. Stability Note
----------------------------------------------------------------------

Once frameworks depend on this specification,
semantic changes become extremely expensive.

Do NOT optimize for convenience.
Do NOT shortcut structure.

Favor clarity, explicitness, and long-term stability.
