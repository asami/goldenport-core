# Consequence (Goldenport Design Reference)

Consequence represents a **unified execution and validation result model**.
It is one of the core architectural ideas of goldenport.

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Consequence exists to unify the following concerns into a single abstraction:

- normal execution results
- validation failures
- semantic errors
- recoverable vs unrecoverable faults
- warnings and additional observations

It deliberately avoids splitting these concerns into:
- Try
- Either
- Validation

----------------------------------------------------------------------  
Core Idea
----------------------------------------------------------------------

Consequence[T] is a value that represents either:

- Success[T]
  - contains a result value
  - may contain warnings or additional conclusions

- Error[T]
  - contains a Conclusion
  - does not contain a result value

Errors are **data**, not control flow.

----------------------------------------------------------------------  
Semantic Characteristics
----------------------------------------------------------------------

Key properties of Consequence:

- It is *not* a simple Either
- It is *not* purely a Validation
- It is *not* an exception wrapper

Instead, it is a **semantic execution algebra** that:

- carries meaning-rich error information
- accumulates conclusions
- allows controlled execution continuation

----------------------------------------------------------------------  
Composition Model
----------------------------------------------------------------------

Consequence supports:

- sequential composition
  - via map / flatMap
- controlled accumulation
  - via Conclusion combination
- applicative-style usage
  - multiple independent validations
  - combined error reporting

Although implemented as a Monad,
its design intent is closer to **Applicative validation with context**.

----------------------------------------------------------------------  
Applicative Interpretation
----------------------------------------------------------------------

Goldenport intentionally supports a pattern where:

- multiple validations are evaluated
- all failures are collected
- conclusions are merged

This avoids early termination while preserving semantic meaning.

This design is especially important for:

- CLI argument validation
- record / schema validation
- configuration loading

----------------------------------------------------------------------  
Error Unification
----------------------------------------------------------------------

Consequence is the *only* error carrier exposed at API boundaries.

As a result:

- Try is not exposed
- ValidationNel is not exposed
- Either is not exposed

All errors are normalized into Conclusion.

----------------------------------------------------------------------  
Scala 3 / cats Interpretation
----------------------------------------------------------------------

When reinterpreted in Scala 3:

- Consequence[T] remains a Monad
- Applicative behavior is preserved intentionally
- Error accumulation is explicit, not implicit

The key rule is:

Do not introduce parallel error abstractions alongside Consequence.

----------------------------------------------------------------------  
Usage Guidance
----------------------------------------------------------------------

Use Consequence when:

- validating user input
- converting raw values into domain objects
- executing operations with semantic failure modes

Avoid using Consequence for:

- internal pure transformations
- performance-critical tight loops

----------------------------------------------------------------------  
Relationship to Conclusion
----------------------------------------------------------------------

Consequence carries **Conclusion** as its semantic error payload.

Conclusion represents:
- what went wrong
- why it matters
- how it should be interpreted

Consequence represents:
- whether execution succeeded
- how results and errors compose

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **conceptual contract** of Consequence.

Implementations may evolve,
but this semantic role must be preserved.
