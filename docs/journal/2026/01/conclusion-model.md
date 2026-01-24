----------------------------------------------------------------------
Conclusion Model (Draft)
----------------------------------------------------------------------

This document defines the conceptual and architectural model of
`Conclusion` in the Goldenport / SimpleModeling core.

The Conclusion Model provides the *upper-level integration* of
error-related semantics, while delegating concrete error information
structure to subordinate specifications such as
`error-information-model.md`.

----------------------------------------------------------------------
Purpose
----------------------------------------------------------------------

The purpose of the Conclusion Model is to explain:

- What `Conclusion` represents as a program-level construct
- Why error-related semantics must be physically propagated
- How `Conclusion` relates to Observation / Interpretation / Disposition
- How evaluation and resolution are intentionally separated from core logic

This document does NOT redefine error vocabularies or classifications.
Those are specified in subordinate documents.

----------------------------------------------------------------------
Position in the Architecture
----------------------------------------------------------------------

Conclusion is a *transportable semantic object*.

It is designed to be:
- created at error detection points
- propagated across layers
- refined through chaining
- evaluated by external projections

Conclusion deliberately does not execute behavior by itself.

----------------------------------------------------------------------
Relationship to Error Information Model
----------------------------------------------------------------------

The internal structure of error-related information carried by
Conclusion is defined by the Error Information Model.

    Conclusion
    └─ Error Information
       ├─ Observation
       ├─ Interpretation
       └─ Disposition

See:
- docs/journal/2026/01/error-information-model.md

This separation ensures that:
- structural semantics are stable
- higher-level reasoning remains extensible
- core logic remains projection-independent

----------------------------------------------------------------------
Conclusion as a Physical Carrier
----------------------------------------------------------------------

`Conclusion` exists to *physically carry* semantic information
through program execution.

It replaces:
- exception-based control flow
- out-of-band logging dependencies
- projection-specific error objects

By carrying structured semantics explicitly, Conclusion enables:
- deterministic evaluation
- reliable propagation
- semantic preservation across boundaries

----------------------------------------------------------------------
Conclusion Chaining
----------------------------------------------------------------------

Conclusions may be chained.

Chaining allows higher layers to:
- refine Interpretation
- override or supplement Disposition
- preserve original Observation intact

Rules:
- Observation is immutable
- Interpretation may be refined
- Disposition may be overridden with stricter or broader policy

Chaining must remain explicit and traceable.

----------------------------------------------------------------------
Evaluation Model
----------------------------------------------------------------------

Conclusion itself does not decide final behavior.

Evaluation is performed by consumers such as:
- CLI frontends
- HTTP / API adapters
- UI layers
- Batch or job runtimes
- Monitoring and alerting systems

Evaluation considers:
- Conclusion content
- runtime policy
- environment configuration
- operational constraints

Evaluation MUST NOT mutate Observation.
It MAY adjust Interpretation and Disposition.

----------------------------------------------------------------------
Resolution Model
----------------------------------------------------------------------

Resolution is the act of committing to concrete actions, such as:

- displaying user messages
- choosing exit codes or HTTP status
- retrying operations
- escalating incidents
- emitting alerts or metrics

Resolution is always projection-specific.

Conclusion provides guidance, not commands.

----------------------------------------------------------------------
Design Principles
----------------------------------------------------------------------

- Fact and meaning must not be conflated
- Error semantics must survive transport
- Runtime behavior must remain configurable
- Core logic must not depend on projections
- Error handling must be auditable and explainable

----------------------------------------------------------------------
Scope of This Document
----------------------------------------------------------------------

This document defines:
- the conceptual role of Conclusion
- its architectural boundaries
- its relationship to subordinate specifications

It intentionally avoids:
- detailed error taxonomies
- concrete handler implementations
- projection-specific policies

Those concerns are addressed in separate documents.
