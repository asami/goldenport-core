# Conclusion (Goldenport Design Reference)

Conclusion represents the **semantic description of an error or abnormal state**.

It is designed to be:
- explainable
- classifiable
- composable

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Conclusion exists to answer:

- What went wrong?
- Why did it happen?
- How should this be handled?
- Who is responsible?
- Is this recoverable?

It separates **error meaning** from **error handling**.

----------------------------------------------------------------------  
Core Responsibility
----------------------------------------------------------------------

Conclusion encapsulates:

- status classification (e.g. HTTP-like code)
- human-readable messages
- machine-readable fault details
- optional exception information
- handling strategies

It does NOT:
- throw exceptions by default
- perform side effects
- choose concrete handlers

----------------------------------------------------------------------  
Semantic Classification
----------------------------------------------------------------------

A Conclusion typically contains:

- status
  - success / client error / server error
- faults
  - argument fault
  - property fault
  - domain fault
- optional strategies
  - retry
  - input correction
  - escalation

This allows the same error to be:
- logged
- displayed
- retried
- escalated

without changing the error itself.

----------------------------------------------------------------------  
Composition
----------------------------------------------------------------------

Conclusions are composable.

When multiple errors occur:

- faults can be accumulated
- messages can be merged
- the most severe status can be selected

This is critical for:
- batch validation
- CLI error reporting
- configuration diagnostics

----------------------------------------------------------------------  
Exception Relationship
----------------------------------------------------------------------

Exceptions are treated as **data sources**, not control flow.

A Conclusion may:
- wrap an exception
- normalize an exception
- discard exception details when inappropriate

Throwing is a *terminal operation*, not the default.

----------------------------------------------------------------------  
Human and Machine Readability
----------------------------------------------------------------------

Conclusion is designed to be:

- readable by humans
- structured for machines
- serializable

This supports:
- CLI output
- REST error responses
- logging
- AI-assisted diagnostics

----------------------------------------------------------------------  
Scala 3 / cats Interpretation
----------------------------------------------------------------------

In Scala 3:

- Conclusion remains a pure data structure
- ErrorCode / Strategy are declarative
- No effect system is embedded in Conclusion

Conclusion should remain independent of:
- IO
- async
- runtime frameworks

----------------------------------------------------------------------  
Relationship to Consequence
----------------------------------------------------------------------

- Conclusion describes *what went wrong*
- Consequence describes *how execution proceeds*

They must not be conflated.

----------------------------------------------------------------------  
ReactionStrategy and CacheStrategy
----------------------------------------------------------------------

Goldenport defines Strategy as *interpretive metadata* attached to Conclusion.

Strategies do not perform actions.
They express **recommended handling semantics** for humans, middleware,
or higher-level orchestration layers.

----------------------------------------------------------------------  
ReactionStrategy
----------------------------------------------------------------------

ReactionStrategy expresses **how a failure should be handled conceptually**.

It answers:
- Should the user fix input?
- Should the system retry?
- Should the issue be escalated?

Defined reactions:

- none
  - no specific guidance
- input
  - client-side correction is expected
- retry
  - transient failure, retry may succeed
- escalate
  - requires human or system-level intervention

ReactionStrategy is:
- derived from Fault semantics
- independent of transport (HTTP, CLI, batch)
- advisory, not imperative

Because of its tight coupling to semantic error handling,
ReactionStrategy fits naturally **inside Conclusion**.

----------------------------------------------------------------------  
CacheStrategy (Design Note)
----------------------------------------------------------------------

CacheStrategy exists as a **future-oriented design hook**.

Original intent:
- provide cache-control hints for:
  - web responses
  - static resources
  - derived artifacts

In practice:
- CacheStrategy was rarely used
- no stable semantics emerged

Design decision:
- keep CacheStrategy as an optional placeholder
- do not require it for core error handling
- allow future reintroduction when cache semantics become explicit

CacheStrategy must remain:
- advisory
- non-operational
- safe to ignore

----------------------------------------------------------------------  
Design Principle
----------------------------------------------------------------------

Strategies must obey these rules:

- They do NOT cause side effects
- They do NOT control execution flow
- They do NOT replace error handling logic

They exist to:
- preserve semantic intent
- guide orchestration
- support observability and UX

Conclusion remains a **value-level semantic summary**,
not a controller.

----------------------------------------------------------------------  
Scala 3 / SimpleModeling Interpretation
----------------------------------------------------------------------

In SimpleModeling:

- ReactionStrategy should be:
  - a small closed ADT
  - colocated with Conclusion
- CacheStrategy should be:
  - optional
  - decoupled from core logic

This preserves goldenport’s insight
while avoiding over-engineering.

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **semantic model** of Conclusion.

Implementations may differ,
but the classification and intent must remain stable.
