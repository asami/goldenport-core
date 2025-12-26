Spec Style Rules｜仕様記述スタイル規約
==================================

status=published
published_at=2025-12-26

# PURPOSE
This document defines the standard style for writing specifications
in SimpleModeling.

The goal is to ensure that specifications are:
- Readable as documents
- Executable as tests
- Authoritative as design artifacts

These rules apply to **Working Specifications**.

---

# CORE PRINCIPLE

A specification in SimpleModeling is not a test of execution,
but a **description of semantic interpretation**.

Specifications define *what a model means*,
not *what a system does at runtime*.

---

# STRUCTURE

Specifications MUST be written using:

- `AnyWordSpec`
- `Matchers`
- `GivenWhenThen`

This combination provides:
- Natural-language structure
- Clear assertions
- Executable specification logging

---

# GIVEN / WHEN / THEN SEMANTICS

The `GivenWhenThen` DSL is mandatory for top-level behavior descriptions.

Each clause has a **SimpleModeling-specific meaning**:

## GIVEN
Describes:
- Model definitions
- Service / operation specifications
- Preconditions for interpretation

`given` MUST NOT:
- Perform execution
- Contain side effects
- Depend on runtime state

---

## WHEN
Describes:
- A semantic request
- Interpretation invocation (e.g. `makeRequest`)

`when` MUST:
- Express *meaningful intent*
- Avoid execution semantics

---

## THEN
Describes:
- The semantic result
- The resolved model object
- Success or failure of interpretation

`then` MUST:
- Assert on model structure or type
- Treat failure as a valid specification outcome

`then` MUST NOT:
- Execute operations
- Perform IO
- Validate runtime behavior

---

# LOGGING AND OUTPUT

The text passed to `given`, `when`, and `then` MUST be written
as human-readable specification sentences.

These sentences are considered part of the specification itself,
since they appear in test execution output.

---

# FAILURE AS SPECIFICATION

Failure cases are first-class specifications.

Specifications SHOULD explicitly define:
- Missing services
- Unknown operations
- Invalid parameter interpretations

A failure result is a **valid semantic outcome**.

---

# STYLE RULES

## MUST
- Use `GivenWhenThen`
- Use explicit `given / when / then` blocks
- Keep one semantic contract per test case
- Assert only on semantic results

## SHOULD
- Keep setup inline within `given`
- Use descriptive sentences in DSL calls
- Treat specs as documentation first

## MUST NOT
- Use `beforeEach` for hidden setup
- Test execution or side effects
- Mix runtime concerns into specifications

---

# EXAMPLES

Typical Working Specifications include:
- `CliEngineSpec`
- `CliLogicSpec`
- `OperationDefinitionSpec`

These specs define the authoritative behavior
of the model interpretation layer.

---

# NOTES

This style intentionally differs from traditional BDD.
While the structure is similar, the intent is semantic,
not behavioral or execution-oriented.
