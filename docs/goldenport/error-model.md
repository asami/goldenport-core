# Error Model and Fault (Goldenport Design Reference)

This document describes the **error model of goldenport**,
centered around the concept of *Fault*.

Fault is the **atomic semantic error unit** used to explain,
classify, and compose failures across the system.

----------------------------------------------------------------------  
Core Philosophy
----------------------------------------------------------------------

Goldenport treats errors as:

- semantic facts
- composable values
- explainable structures

Errors are **not control flow** and **not merely messages**.

The error model is explicitly designed to support:
- CLI feedback
- configuration diagnostics
- domain validation
- system failure analysis

----------------------------------------------------------------------  
Fault: Atomic Error Unit
----------------------------------------------------------------------

Fault represents a single, well-defined semantic problem.

Each Fault provides:

- a stable name
- a localized human-readable message
- a machine-readable reaction category
- an implicit status code
- structured properties for inspection

Faults are designed to be:
- immutable
- serializable
- composable

----------------------------------------------------------------------  
Fault vs Exception
----------------------------------------------------------------------

Important distinction:

- Fault
  - semantic error description
  - value-level representation
  - does not cause side effects

- Exception
  - execution control mechanism
  - optional terminal representation

Exceptions may be *derived from* Faults,
but Faults exist independently.

----------------------------------------------------------------------  
Reaction Semantics
----------------------------------------------------------------------

Each Fault declares a **Reaction**, describing how the system should respond.

Typical reactions include:

- ClientInput
  - user-provided input is invalid
  - correction is possible

- SystemDefect
  - internal error
  - requires investigation

Reactions are declarative.
They do not perform actions.

----------------------------------------------------------------------  
Implicit Status Code
----------------------------------------------------------------------

Each Fault declares an implicit status code.

This enables:

- HTTP response mapping
- CLI exit code determination
- severity comparison

Status codes are:
- derived from fault semantics
- not chosen by callers

----------------------------------------------------------------------  
Fault Categories
----------------------------------------------------------------------

Goldenport defines several major Fault categories.

----------------------------------------------------------------------
ArgumentFault
----------------------------------------------------------------------

Represents invalid or missing user input.

Examples:
- InvalidArgumentFault
- MissingArgumentFault
- EmptyArgumentFault
- TooManyArgumentsFault
- SyntaxErrorFault
- FormatErrorFault

Characteristics:
- reaction: ClientInput
- status: typically BadRequest

----------------------------------------------------------------------
ValueDomainFault
----------------------------------------------------------------------

Represents datatype or multiplicity violations.

Examples:
- ValueDomainValueFault
- ValueDomainMultiplicityFault
- ValueDomainDatatypeFault

Characteristics:
- reaction: ClientInput
- tied to datatype / schema validation

----------------------------------------------------------------------
PropertyFault
----------------------------------------------------------------------

Represents structural or configuration-level property issues.

Examples:
- InvalidPropertyFault
- MissingPropertyFault
- ValueDomainPropertyFault

These often indicate:
- configuration mismatch
- schema-property inconsistency

----------------------------------------------------------------------
ResultFault
----------------------------------------------------------------------

Represents invalid results produced by the system.

Examples:
- ValueDomainResultFault

Characteristics:
- reaction: SystemDefect
- indicates internal inconsistency

----------------------------------------------------------------------
IoFault
----------------------------------------------------------------------

Represents I/O related failures.

Examples:
- DatabaseIoFault
- NetworkIoFault
- FileIoFault
- SystemIoFault
- SubsystemIoFault
- ResourceNotFoundFault

Characteristics:
- reaction: SystemDefect
- status: InternalServerError

----------------------------------------------------------------------
Defect
----------------------------------------------------------------------

Represents unrecoverable system defects.

Examples:
- IllegalStateDefect
- IllegalConfigurationDefect
- NoReachDefect

These indicate:
- violated invariants
- impossible states
- programming or configuration errors

----------------------------------------------------------------------  
Fault Composition: Faults
----------------------------------------------------------------------

Faults represents a collection of Fault.

Capabilities include:

- accumulation of multiple faults
- message aggregation
- severity comparison
- status code inference

Faults enables:

- applicative-style validation
- comprehensive CLI error reporting
- batch diagnostics

----------------------------------------------------------------------  
Message and Localization
----------------------------------------------------------------------

Fault messages are:

- internationalized
- template-based
- parameterized

Localization is a first-class concern,
not an afterthought.

----------------------------------------------------------------------  
Relationship to Conclusion
----------------------------------------------------------------------

Faults are aggregated into Conclusion.

- Fault
  - atomic semantic error

- Faults
  - collection of semantic errors

- Conclusion
  - high-level classification and strategy

Conclusion uses Faults to:
- determine status
- decide reaction strategy
- produce final error representation

----------------------------------------------------------------------  
Scala 3 / cats Interpretation
----------------------------------------------------------------------

When reinterpreted in Scala 3:

- Fault should be a sealed hierarchy
- Fault data should be immutable
- Localization should be declarative
- No side effects in Fault construction

Fault should integrate with:
- Consequence
- Conclusion

without introducing parallel error abstractions.

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **semantic error model**.

Implementations may vary,
but the classification and intent must remain stable.
