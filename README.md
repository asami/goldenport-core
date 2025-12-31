# SimpleModeling Library

## What This Is

SimpleModeling Library is a core semantic model library for model-driven systems.
It defines long-lived meaning for protocol, datatype, and operation semantics so
generated and handwritten code can share a stable runtime vocabulary.

This repository is not an application framework. It is the semantic substrate
that keeps meaning stable across tools, runtimes, and adapters.

## Design Philosophy (Short)

- Executable Specification is first-class documentation
- Meaning > mechanics
- Functional programming oriented (Scala)
- Given/When/Then + Property-Based Testing
- Example-based tests are minimized

## How to Read This Repository (for Humans)

1. `README.md`
2. `docs/rules/`
3. `docs/spec/`
4. `docs/design/`
5. `src/test/scala/`
6. `src/main/scala/`

## Where the Specifications Live

- Executable Specifications live in `src/test/scala`.
- Specs are organized by semantic responsibility (e.g., `org.goldenport.protocol`, `org.goldenport.scenario`).
- Detailed style rules live in `docs/spec/spec-style.md`.
- Test policy lives in `docs/spec/test-policy.md`.

## For AI / Agents

AI/Agent guidance is in `AGENTS.md`, not here.
`AGENTS.md` is the canonical entry point for automated agents.

## Related Docs (Details Live Here)

- Error and observation model: `docs/spec/error-observation-integration.md`
- Error model exploration notes: `docs/notes/error-model-exploration.md`
- Design and integration boundaries: `docs/design/`
- Spec style and test policy: `docs/spec/spec-style.md`, `docs/spec/test-policy.md`
- Rules and conventions: `docs/rules/` (see `docs/rules/type-modeling.md`)

## Context Model (Current)

- EnvironmentContext, VirtualMachineContext, and I18nContext are independent contexts.
- ExecutionContext composes those contexts and adds no new semantics.
- Bootstrap and factory responsibilities live in CNCF / infrastructure, not core.
- See `docs/spec/context-overview.md` and related specs under `docs/spec/`.

## Summary

- This repository defines semantic meaning, not runtime behavior.
- Specifications live as executable code under `src/test/scala`.
- Given/When/Then + PBT is the baseline spec style.
- Read rules, spec, and design docs before implementation.

## Further Reading

The following topics are intentionally not explained in this README.
They are documented in detail in the corresponding documents below.

### Error and Observation Model
Semantic error vocabulary and observation-based error handling.
See:
- `docs/spec/error-observation-integration.md`
- `docs/notes/error-model-exploration.md`

### Framework and Application Integration
Boundaries and responsibilities between this library, frameworks, and applications.
See:
- `docs/design/`

### Test Policy and Specification Style
Executable Specification philosophy, test policy, and specification style rules.
See:
- `docs/spec/test-policy.md`
- `docs/spec/spec-style.md`

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
