# Datatype (Goldenport Design Reference)

Datatype represents the **semantic meaning of values**.

It defines how raw input is:
- interpreted
- validated
- converted

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Datatype exists to answer:

- What kind of value is this?
- What representations are allowed?
- How is the value validated?
- How is it converted into a typed form?

Datatype is independent of:
- storage
- transport
- execution context

----------------------------------------------------------------------  
Core Characteristics
----------------------------------------------------------------------

A Datatype defines:

- logical type (string, number, date, etc.)
- validation rules intrinsic to the type
- conversion from raw input to typed instance
- formatting or representation hints

Datatype does NOT:
- know column names
- handle multiplicity
- manage constraints external to the type

----------------------------------------------------------------------  
Intrinsic Validation
----------------------------------------------------------------------

Datatype validation includes:

- syntax checks
- format checks
- range checks intrinsic to the type

Examples:
- integer parsing
- date parsing
- boolean normalization

----------------------------------------------------------------------  
Conversion Responsibility
----------------------------------------------------------------------

Datatype converts validated input into:

- a typed value object
- or a domain-specific representation

Conversion failures are semantic errors,
not system failures.

----------------------------------------------------------------------  
Unification and Inference
----------------------------------------------------------------------

Goldenport supports:

- datatype inference from values
- datatype unification across multiple observations

This enables:
- schema inference
- data-driven modeling

These capabilities are optional extensions.

----------------------------------------------------------------------  
Datatype vs Constraint
----------------------------------------------------------------------

Important separation:

- Datatype
  - intrinsic properties of a type

- Constraint
  - contextual rules imposed by schema or domain

This separation keeps Datatype reusable.

----------------------------------------------------------------------  
Scala 3 / SimpleModeling Interpretation
----------------------------------------------------------------------

In SimpleModeling:

- Datatype should be an algebraic data type
- Validation returns Consequence
- Conversion must be total or explicitly failing

Datatype should remain:
- pure
- deterministic
- side-effect free

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **semantic contract** of Datatype.

Future extensions must preserve:
- purity
- clarity
- separation from execution concerns
