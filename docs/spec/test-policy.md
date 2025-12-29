======================================================================
Test Specification — SimpleModeling Library
======================================================================

Status: Normative
Scope: simplemodeling-lib (core semantic library)

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document defines the normative test policy for the
SimpleModeling Library.

Tests in this library are treated as executable working specifications
that capture and stabilize the semantic meaning of core domain
primitives.

The primary goal of testing is to protect long-lived meaning,
not to verify short-term implementation behavior.

----------------------------------------------------------------------
2. Fundamental Principles
----------------------------------------------------------------------

2.1 Tests as Working Specifications

Tests describe:
  - semantic intent
  - invariants and properties
  - stable meaning of public APIs

Tests do NOT describe:
  - control flow
  - internal branching
  - framework-specific behavior

2.2 Implementation-First Policy

This library does not follow classic test-first TDD.

The standard order is:

  1. Semantic design and modeling
  2. Implementation
  3. Test writing to record properties and invariants

Tests are written after behavior becomes concrete, in order to
stabilize meaning rather than to drive design.

----------------------------------------------------------------------
3. Test Layer
----------------------------------------------------------------------

Only a single test layer is used: Executable Specifications.

Executable Specifications (src/test)

Characteristics:
  - semantic-level (protocol and scenario semantics)
  - deterministic
  - no external dependencies
  - always executed in CI

Property-Based Testing (PBT) is the primary testing technique.

----------------------------------------------------------------------
4. Writing Guidelines
----------------------------------------------------------------------

Tests SHOULD focus on:
  - public domain primitives
  - invariants
  - idempotence
  - semantic stability

Tests MUST NOT focus on:
  - private helpers
  - implementation details
  - ad hoc example-based integration checks
  - non-semantic execution mechanics

Scenario and usecase Executable Specifications are allowed and recommended
when they capture semantic intent using Given/When/Then and PBT.

----------------------------------------------------------------------
5. Summary
----------------------------------------------------------------------

- Tests are executable working specifications
- Meaning is protected over mechanics
- PBT is the primary approach
- Example-based integration tests are minimized
- Scenario/usecase Executable Specifications are part of the standard practice

======================================================================
