SimpleModeling Library
=====================

SimpleModeling Library is a core semantic model library
for model-driven software systems.

It provides framework-agnostic, long-lived domain primitives
that form a shared semantic foundation across:

    - domain modeling
    - application logic
    - configuration loading
    - error handling
    - observability
    - cloud-native runtimes
    - AI-assisted development

In addition, this library serves as the **runtime foundation**
for artifacts automatically generated from CML
(Cozy Modeling Language).

----------------------------------------------------------------------
Design Philosophy
----------------------------------------------------------------------

SimpleModeling Library is built on the following principles:

    - Model first: define meaning before behavior
    - Separation of concerns: fact, judgment, and reaction are distinct
    - Framework neutrality: usable standalone or with any framework
    - Long-term stability: semantic vocabulary outlives APIs
    - Explicit structure over convenience

The library defines what things mean,
not what systems should do.

----------------------------------------------------------------------
Conceptual Overview
----------------------------------------------------------------------

The library distinguishes three fundamental layers
for describing runtime behavior:

    - Phenomenon
    - Observation
    - Conclusion / Consequence

This separation is intentional:

    - Observation describes what happened
    - Conclusion represents a judgment
    - Consequence represents an outcome

No layer performs execution by itself.

----------------------------------------------------------------------
CML-Generated Runtime Artifacts
----------------------------------------------------------------------

SimpleModeling Library functions as the **runtime support library**
for code generated from CML (Cozy Modeling Language).

CML is used to describe:

    - domain entities and values
    - lifecycle and state machines
    - rules and constraints
    - domain services and operations
    - observation and error semantics

Artifacts generated from CML rely on simplemodeling-lib
to provide:

    - shared semantic types
    - observation primitives
    - conclusion / consequence control-flow
    - stable error and classification vocabulary

In this sense, simplemodeling-lib is:

    - not an application framework
    - not code generation output itself
    - but the runtime substrate on which
      generated artifacts execute

This allows generated code to remain:

    - thin
    - declarative
    - free of framework coupling

----------------------------------------------------------------------
Base DataType Profile Compliance
----------------------------------------------------------------------

SimpleModeling Library follows the *SimpleModeling Base DataType Profile*,
which defines the normative vocabulary of basic data types
used in domain models.

The profile specifies:

    - semantic meaning of data types
    - value ranges and constraints
    - correspondence to Scala / Java types
    - reference mapping to XML Schema types
    - which types require SimpleModeling-specific support

This library implements only those basic data types
that cannot be adequately expressed by
Scala or Java standard library types alone.

Implementation rules are as follows:

    - Data types already provided by Scala or Java standard libraries
      MUST be used as-is and MUST NOT be wrapped.
    - SimpleModeling-specific data types are introduced only when
      semantic meaning or explicit value constraints are required.
    - XML Schema types are treated as reference information only
      and MUST NOT be reified as Scala types.
    - This library does NOT provide:
        - arithmetic or domain-specific operations
        - validation frameworks
        - serialization or transport semantics

This policy ensures that simplemodeling-lib remains
minimal, semantic, and framework-independent.

The Base DataType Profile itself is defined in
the SimpleModeling documentation site and is considered
normative for this library.

----------------------------------------------------------------------
Core Concepts
----------------------------------------------------------------------

Observation
-----------

Observation represents a **factual record**
of something that occurred during execution.

Properties:

    - descriptive, not prescriptive
    - structured and machine-readable
    - compatible with observability systems
    - free of execution or reaction semantics

Observation unifies information traditionally scattered across:

    - logs
    - traces
    - error records
    - audit trails

Observation is defined in:

    org.simplemodeling.observation

----------------------------------------------------------------------
Conclusion
----------------------------------------------------------------------

Conclusion represents a **judgment** made about an observation.

It classifies and contextualizes what was observed,
but does not execute behavior.

Typical responsibilities:

    - classification
    - reference to Observation
    - optional causal linkage

Conclusion is intentionally minimal
and designed to be interpreted by higher layers.

----------------------------------------------------------------------
Consequence[T]
----------------------------------------------------------------------

Consequence[T] represents the **outcome of an operation**.

It is the primary control-flow abstraction
used by generated and handwritten code alike.

Forms:

    - Success(result, conclusion)
    - Failure(conclusion)

Rules:

    - expected failures are values, not exceptions
    - all failures must carry a Conclusion
    - exceptions are reserved for defects only

----------------------------------------------------------------------
Error Modeling
----------------------------------------------------------------------

Error handling in simplemodeling-lib follows these rules:

    - errors are observations, not exceptions
    - interpretation is context-dependent
    - reaction belongs to frameworks, not core

The library provides:

    - semantic error vocabulary
    - minimal classification structures
    - extension points for interpretation

Generated CML runtime code depends on these guarantees
to remain framework-independent.

----------------------------------------------------------------------
Integration with Frameworks and Applications
----------------------------------------------------------------------

simplemodeling-lib integrates with runtime systems
via explicit extension points.

Frameworks
----------

Cloud-Native Component Framework (CNCF) is the primary
framework-level integrator of simplemodeling-lib.

CNCF is responsible for:

    - interpreting Conclusion and ErrorStrategy
    - deciding retry, escalation, or abort behavior
    - projecting Observation into logs, traces, and metrics
    - providing execution and lifecycle semantics

simplemodeling-lib itself remains framework-independent
and never performs interpretation or execution.

Applications
------------

Semantic Integration Engine (SIE) is **not a framework**.

SIE is an application built **on top of CNCF** and therefore
uses simplemodeling-lib **indirectly via CNCF**.

In this structure:

    simplemodeling-lib
        ↓
    CNCF (framework)
        ↓
    SIE (application)

Applications built on CNCF rely on the guarantees provided by:

    - simplemodeling-lib (semantic core)
    - CNCF (interpretation and execution)

but do not redefine or extend core semantics directly.

----------------------------------------------------------------------
Normative Integration Contract
----------------------------------------------------------------------

The integration contract between simplemodeling-lib
and framework-level runtimes (such as CNCF)
is defined in:

    docs/spec/error-observation-integration.md

This specification is normative for framework integration.

Applications such as SIE MUST conform to this contract
indirectly by using CNCF-provided mechanisms.

----------------------------------------------------------------------
Exploratory Design Notes
----------------------------------------------------------------------

Some aspects of the error / observation model
are intentionally exploratory and usage-driven.

Design context, historical experience,
and empirical findings are preserved in:

    docs/notes/error-model-exploration.md

These notes are:

    - non-normative
    - evolving
    - intended to preserve design memory

----------------------------------------------------------------------
Intended Use Cases
----------------------------------------------------------------------

    - Domain-driven design
    - CML-based code generation
    - Generated runtime execution
    - Configuration loading and validation
    - CLI and batch execution
    - Cloud-native applications
    - Observability and diagnostics
    - AI-assisted analysis and tooling

----------------------------------------------------------------------
Non-Goals
----------------------------------------------------------------------

This library is not:

    - a logging framework
    - an observability backend
    - a web / HTTP abstraction
    - a retry or workflow engine
    - a code generation tool itself

It defines meaning, not mechanisms.

----------------------------------------------------------------------
Status
----------------------------------------------------------------------

This library is under active development.

Semantic clarity and structural correctness
are prioritized over short-term convenience.

Once frameworks or generated runtimes depend on a concept,
semantic changes are treated as extremely costly
and must be carefully justified.

----------------------------------------------------------------------
Test Policy
----------------------------------------------------------------------

Tests in simplemodeling-lib are treated as **executable working specifications**.

They are not written primarily to verify implementation correctness,
but to capture and stabilize the intended semantic meaning of
public domain primitives.

Development Order

Implementation is done first.
Tests are written afterward to record properties and invariants
once behavior becomes concrete.

This project does not follow classic test-first TDD.

Test Characteristics

- Tests are unit-level only
- Tests focus on public APIs and stable semantic properties
- Private helpers and implementation details are not tested
- Tests describe invariants, idempotence, and semantic stability
- Tests act as long-lived documentation of intent

Test Structure

All tests are located under:

    src/test

Only unit and property-oriented tests are included in this module.
Scenario, integration, or execution-level tests are intentionally excluded.

Test Style and Tools

- ScalaTest AnyWordSpec is used as the base style
- Property-Based Testing (PBT) is the primary testing approach
- ScalaCheck is used via ScalaCheckDrivenPropertyChecks

Tests serve as a semantic safety net,
protecting long-term meaning rather than short-term behavior.
