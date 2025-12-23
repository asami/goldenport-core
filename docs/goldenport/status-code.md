# StatusCode (Goldenport Design Reference)

StatusCode represents the **coarse-grained classification of execution results**.
It provides a stable, interoperable way to express success and failure states.

In goldenport, StatusCode is deliberately designed to be:
- HTTP-compatible
- semantically extensible
- composable with finer-grained error information

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

StatusCode exists to answer:

- Was the operation successful?
- If not, what *class* of failure occurred?
- How severe is the failure?
- How should external systems interpret this result?

StatusCode is a **classification layer**, not a diagnostic layer.

----------------------------------------------------------------------  
Core Structure
----------------------------------------------------------------------

A StatusCode consists of:

- code
  - integer value (HTTP-like)
- detail (optional)
  - semantic refinement
- application (optional)
  - application-specific code (reserved)
- messageOption (optional)
  - override or localized message
- externalService (optional)
  - nested status from downstream systems

This structure allows StatusCode to act as a **boundary object**
between internal semantics and external protocols.

----------------------------------------------------------------------  
HTTP Compatibility
----------------------------------------------------------------------

StatusCode intentionally aligns with HTTP status semantics:

- 2xx: success
- 3xx: redirection / non-failure
- 4xx: client-side error
- 5xx: server-side error

The method `isSuccess` reflects this range-based classification.

This alignment enables:
- REST API responses
- CLI exit code mapping
- log severity inference

----------------------------------------------------------------------  
DetailCode Integration
----------------------------------------------------------------------

StatusCode may carry an optional DetailCode.

DetailCode refines the meaning of a status without changing its class.

Examples:
- BadRequest + Argument
- BadRequest + ArgumentSyntax
- InternalServerError + Config
- InternalServerError + NoReach

This separation allows:
- stable top-level classification
- expressive internal diagnostics

----------------------------------------------------------------------  
forConfig Transformation
----------------------------------------------------------------------

StatusCode provides `forConfig` to support configuration contexts.

Behavior:
- success codes remain unchanged
- failure codes are normalized to InternalServerError
- detail codes are transformed to their configuration variants

This supports:
- reuse of validation logic in config loading
- consistent treatment of configuration errors

----------------------------------------------------------------------  
StatusCode vs Fault
----------------------------------------------------------------------

Important separation:

- StatusCode
  - coarse-grained classification
  - protocol-facing
  - severity-oriented

- Fault
  - fine-grained semantic cause
  - domain-oriented
  - explanatory

StatusCode answers *how bad*,
Fault answers *what went wrong*.

----------------------------------------------------------------------  
Composition and Propagation
----------------------------------------------------------------------

StatusCode supports propagation scenarios:

- wrapping downstream service failures
- embedding external service status
- overriding messages when necessary

This makes StatusCode suitable for:
- layered architectures
- service orchestration
- proxy components

----------------------------------------------------------------------  
Relationship to Conclusion
----------------------------------------------------------------------

StatusCode is embedded in Conclusion.

- Conclusion determines:
  - which StatusCode applies
  - how Faults influence severity
  - what strategy should be suggested

StatusCode alone is insufficient to explain an error,
but it provides the necessary top-level signal.

----------------------------------------------------------------------  
Scala 3 / SimpleModeling Interpretation
----------------------------------------------------------------------

In Scala 3:

- StatusCode should remain a pure value object
- HTTP alignment should be preserved
- DetailCode should be modeled explicitly
- No execution logic should be embedded

StatusCode should be usable without:
- web frameworks
- IO
- async runtimes

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **classification semantics** of StatusCode.

All future implementations must preserve:
- HTTP compatibility
- separation from Fault semantics
- stability at system boundaries
