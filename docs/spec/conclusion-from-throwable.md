Conclusion.from(Throwable) Specification
=======================================

----------------------------------------------------------------------
Status
----------------------------------------------------------------------

This document defines the **normative specification** of
`Conclusion.from(Throwable)`.

Breaking changes to the semantics defined here
constitute a **spec-breaking change**.

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

`Conclusion.from(Throwable)` exists to:

- Contain unexpected system failures (Defects)
- Translate thrown exceptions into structured failure values
- Prevent exception leakage into domain and semantic layers

This method is a **boundary construct**.

It MUST NOT be used to represent domain failures.

----------------------------------------------------------------------
2. Semantic Classification
----------------------------------------------------------------------

A Throwable handled by `Conclusion.from(Throwable)` is classified as:

- **Defect**
- **System-level failure**
- **Non-domain error**

No domain meaning MUST be inferred from the Throwable.

----------------------------------------------------------------------
3. Input Constraints
----------------------------------------------------------------------

Input:
- Any instance of `Throwable`

Preconditions:
- None

`null` input:
- MUST be treated as a Defect
- MUST result in a valid `Conclusion`

----------------------------------------------------------------------
4. Output Semantics
----------------------------------------------------------------------

`Conclusion.from(Throwable)` MUST return a `Conclusion`
with the following properties.

----------------------------------------------------------------------
4.1 Observation
----------------------------------------------------------------------

The generated `Observation` MUST:

- Indicate a Defect / Exception cause kind
- Contain minimal information only
- Preserve the original Throwable as opaque input

The Observation MUST NOT:

- Encode domain semantics
- Perform exception-type classification
- Depend on framework-specific exception hierarchies

----------------------------------------------------------------------
4.2 Conclusion
----------------------------------------------------------------------

The generated `Conclusion` MUST:

- Represent a failure state
- Be suitable for `Consequence.Failure`
- Be deterministic

WebCode:
- MUST be `InternalError` (500-equivalent)

Severity:
- MUST be `Error` or higher

Message:
- MUST be generic and non-domain-specific
- MUST NOT expose internal details by default

----------------------------------------------------------------------
5. Prohibited Behavior
----------------------------------------------------------------------

`Conclusion.from(Throwable)` MUST NOT:

- Be used for domain failures
- Be invoked inside domain logic
- Perform business-rule interpretation
- Map specific exception types to domain meanings
- Trigger logging, retries, or side effects

----------------------------------------------------------------------
6. Allowed Behavior
----------------------------------------------------------------------

`Conclusion.from(Throwable)` MAY:

- Capture the Throwable as opaque diagnostic data
- Be used at system boundaries (CLI, REST, MCP, adapters)
- Be composed into `Consequence.Failure`

----------------------------------------------------------------------
7. Usage Rule
----------------------------------------------------------------------

Correct usage:

- Boundary layers
- Adapter code
- Infrastructure glue
- Last-resort failure containment

Incorrect usage:

- Domain services
- Domain entities
- Validation logic
- Business rule evaluation

----------------------------------------------------------------------
8. Relationship to Other Specifications
----------------------------------------------------------------------

- Defect vs Domain Failure distinction:
  docs/notes/defect-vs-domain-failure.md

- Error handling rules:
  RULE.md / Error Handling and Result Conventions

----------------------------------------------------------------------
9. Design Rationale
----------------------------------------------------------------------

This specification intentionally keeps `Conclusion.from(Throwable)`:

- Semantically weak
- Domain-agnostic
- Predictable
- Safe as a last-resort mechanism

Any enrichment of error meaning must occur
*before* an exception is thrown,
not during exception handling.

----------------------------------------------------------------------
10. Final Rule
----------------------------------------------------------------------

`Conclusion.from(Throwable)` exists to **contain defects**, not to explain them.

If domain meaning is required,
exceptions must not be used.
