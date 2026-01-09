---
status = draft
scope = goldenport-core error semantics

# Core Error Semantics

## 1. Purpose

This document defines the **semantic error model of goldenport-core**.

Its purpose is to precisely document the meaning and structure of:
- `Consequence`
- `Conclusion`
- `Observation`
- error causes and classifications
- detail error codes

This note freezes the core-level error semantics
before entering CML and CRUD-oriented development.

No runtime projections (CLI, HTTP, client) are defined here.

---

## 2. Scope and Non-Scope

### In Scope
- Core result and error abstractions
- Domain failure vs defect semantics
- Error cause taxonomy
- Observation data model
- Detail error code design and construction rules

### Out of Scope
- CLI exit codes
- HTTP status mapping
- Logging or emission behavior
- Client-facing error formats
- Workflow or job semantics

---

## 3. Core Result Model

### 3.1 Consequence

`Consequence[T]` is the unified result type in goldenport-core.

- Represents either success or failure
- Failures carry a `Conclusion`
- Exceptions are not propagated beyond `Consequence` boundaries

`Consequence` is the only supported mechanism
for expressing failure in core logic.

---

### 3.2 Conclusion

`Conclusion` represents the **semantic meaning of a failure**.

A `Conclusion` contains:
- A status (web code and strategy)
- An associated `Observation`

`Conclusion` is the canonical container
used by higher layers to interpret failures.

---

## 4. Domain Failure vs Defect

Core explicitly distinguishes between:

### Domain Failure
- Expected failures within domain or validation rules
- Represented using appropriate `Cause` values
- Never treated as system defects

### Defect
- Unexpected or internal failures
- Typically derived from thrown exceptions
- Represented using `CauseKind.Defect`

This distinction is fundamental and must not be bypassed.

---

## 5. Observation Model

### 5.1 Observation Role

`Observation` records an observed failure or abnormal condition.

It captures:
- What happened
- Why it happened
- How it should be classified

Observation is **descriptive**, not interpretive.

---

### 5.2 Cause and Classification

Core provides a fixed taxonomy:

- `Cause`
  - ValidationError
  - Argument(Missing / Redundant / ValidationError)
  - InvalidState
  - Configuration
  - Timeout
  - ResourceExhausted
  - DataCorruption
  - Unknown
- `CauseKind`
  - Fault
  - Defect
  - Anomaly
- `ErrorStrategy`
  - None
  - Retry
  - InputRequired
  - Escalate

These types form the controlled vocabulary
for expressing error semantics.

---

## 6. Detail Error Code Design

### 6.1 Motivation

Human-readable messages are unstable and ambiguous.

Core therefore defines **detail error codes**
as the machine-readable representation of error semantics.

Detail error codes must be:
- Stable
- Structured
- Deterministically generated

---

### 6.2 Separation of Message and Code

- Messages are intended for humans
- Codes are intended for machines (clients, automation, AI)

Clients must rely on codes, not messages.

---

## 7. Detail Error Code Structure

Detail error codes follow a structured, dot-separated format:

```
<kind>.<cause>.<subject>.<reason>.<detail?>
```

Examples:
- `fault.validation.argument.missing.name`
- `fault.validation.value_domain.out_of_range.age`
- `defect.internal.exception.null_pointer`

---

### 7.1 Code Components

- **kind**
  - Derived from `CauseKind`
  - Examples: `fault`, `defect`, `anomaly`

- **cause**
  - Derived from `Cause`
  - Examples: `validation`, `argument`, `state`, `config`

- **subject**
  - Optional domain or parameter identifier
  - Examples: `name`, `age`, `email`

- **reason**
  - Optional classification detail
  - Examples: `missing`, `invalid`, `out_of_range`

- **detail**
  - Optional, implementation-specific
  - Must not affect semantic meaning

---

## 8. Code Construction Rules

- Detail codes are constructed deterministically
- The same semantic error must always produce the same code
- Missing components are omitted, not replaced with placeholders
- Free-form text is not allowed in code components

---

## 9. Exception Mapping Policy

Thrown exceptions are treated as defects.

`Conclusion.from(Throwable)` must:
- Classify the error as `CauseKind.Defect`
- Produce a detail code of the form:
  ```
  defect.internal.exception.<ExceptionClass>
  ```

This ensures that defects are always machine-identifiable.

---

## 10. Explicit Boundaries

Core explicitly does NOT define:
- Exit codes
- HTTP status codes
- Logging or emission policies
- Client-visible error schemas

These are the responsibility of higher layers.

---

## 11. Phase Boundary Declaration

This document freezes core-level error semantics.

After this point:
- Core error meaning must not be reinterpreted
- CML and CRUD layers must conform to this model
- Only additive extensions are allowed

---

END OF DOCUMENT
---
