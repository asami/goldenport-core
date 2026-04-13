# Consequence Design

## Overview

Consequence is the unified result abstraction used across SimpleModeling.
It intentionally integrates multiple result semantics—exception handling,
validation, and sequential computation—into a single explicit type.

The design goal is to avoid fragmentation into Try, Either, Validated,
or ad-hoc result types, while still preserving their distinct meanings
through different APIs and usage styles.

---

## Design Principles

1. One Type, Multiple Meanings

Consequence[T] represents the outcome of an operation, not just success/failure.

It subsumes:

- Try-like semantics  
  Exception capture at unsafe boundaries

- Validation semantics  
  Error accumulation via applicative composition

- Monad semantics  
  Sequential, fail-fast computation

These meanings are not mixed implicitly.
They are selected explicitly by the APIs you use.

---

## Core APIs and Their Semantics

### Success / Failure

Consequence.Success[T]  
Consequence.Failure[T]

- Failure contains a Conclusion, not an exception
- A Conclusion may represent multiple aggregated errors

### Semantic Failure Constructors

Recurring failure meanings must be exposed as semantic utility methods.

Examples:

- `notImplemented(message)`
- `securityPermissionDenied(...)`
- `argumentMissingInput(...)`

The method name should describe the error meaning.  Callers should not need
to assemble taxonomy, cause, facets, and status for common cases.

`Consequence.fail(...)` and the corresponding `Conclusion` constructor forms
remain the low-level escape hatch for application-specific structured
failures when no semantic utility exists yet.  They are not the preferred API
for recurring framework failures.

Legacy `failXxx(...)` aliases are deprecated.  New code should use the
semantic utility name directly, or add a semantic utility before introducing
another repeated low-level failure shape.

---

## Try Semantics (Exception Boundary)

Consequence { unsafeComputation }  
Consequence.run { consequenceComputation }

Purpose:

- Capture NonFatal exceptions
- Normalize them into Conclusion
- Define explicit unsafe boundaries

This replaces ad-hoc Try usage.

---

## Validation / Applicative Semantics (Error Aggregation)

### zip-based APIs (Primary)

zip  
zipWith  
zip3With  
zipN

Example:

take_string("id")
  .zip3With(
    take_string("name"),
    get_datetime("birthday")
  )(User(_, _, _))

Characteristics:

- Order-independent
- Multiple failures are aggregated
- Intended for semantic interpretation, not control flow

This corresponds to Validation + Applicative semantics.
Error aggregation is the primary reason to choose applicative style.

This is the preferred style for request interpretation and domain construction.

---

## Monad / Sequential Semantics (Fail-Fast)

map  
flatMap  
for { ... } yield ...

Example:

for {
  id   <- take_string("id")
  name <- take_string("name")
} yield User(id, name)

Characteristics:

- Fail-fast
- Order-dependent
- Stops at the first failure

This is equivalent to Either-like semantics.

Sequential style is supported when fail-fast is required.

---

## cats Interoperability

Consequence provides:

given Monad[Consequence]

This allows:

- Use with cats APIs
- mapN / Apply when integrating with cats-based code

Example:

(take_string("id"), take_string("name")).mapN(User(_, _))

cats support is optional and non-authoritative.
The canonical APIs remain zip*.

---

## Summary

Concern                    API
-------------------------  -------------------------------
Exception boundary         Consequence.apply, run
Validation / aggregation   zip*
Sequential computation    map, flatMap
cats integration           Monad[Consequence]
Symbolic DSL               Not provided

---

## Design Rationale (One Sentence)

Consequence is designed so that the meaning of computation
is visible from the method names, not hidden behind operators
or imports.
