# Validation (Goldenport Design Reference)

Validation represents the **controlled transformation of raw input
into semantically valid domain values**.

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Validation exists to:

- prevent invalid data from entering the system
- provide meaningful error feedback
- support batch and CLI-style validation
- unify error handling across layers

----------------------------------------------------------------------  
Validation Flow
----------------------------------------------------------------------

Typical validation flow:

1. raw input (often string-based)
2. schema lookup
3. column-based validation
4. datatype validation
5. constraint validation
6. value conversion
7. domain object construction

Each step may produce semantic errors.

----------------------------------------------------------------------  
Early Validation Principle
----------------------------------------------------------------------

Goldenport emphasizes early validation:

- invalid input should be rejected as early as possible
- especially at CLI and configuration boundaries

This improves:
- security
- performance
- user experience

----------------------------------------------------------------------  
Applicative Style
----------------------------------------------------------------------

Validation is intentionally designed to support:

- parallel validation of independent inputs
- accumulation of all validation errors
- non-short-circuiting behavior

This is critical for:
- CLI argument validation
- configuration diagnostics
- record validation

----------------------------------------------------------------------  
Error Accumulation
----------------------------------------------------------------------

Instead of failing fast:

- all relevant errors are collected
- semantic context is preserved
- users receive comprehensive feedback

This is enabled by:
- Conclusion composition
- Consequence accumulation

----------------------------------------------------------------------  
Unified Error Model
----------------------------------------------------------------------

Validation does NOT use:

- Try
- Either
- separate Validation types

All validation results are expressed as:

- Consequence[T]
- with Conclusion as error payload

This ensures consistency across the system.

----------------------------------------------------------------------  
Security Considerations
----------------------------------------------------------------------

Validation is a security boundary.

It must prevent:
- oversized inputs
- malformed values
- injection-like patterns

Early rejection reduces attack surface.

----------------------------------------------------------------------  
Scala 3 / cats Interpretation
----------------------------------------------------------------------

In Scala 3:

- Validation logic should be written in applicative style
- Consequence serves as the unifying abstraction
- No parallel error abstractions should be introduced

Cats may be used internally,
but Consequence remains the public boundary.

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **validation philosophy and model**.

All validation implementations must align with these principles.
