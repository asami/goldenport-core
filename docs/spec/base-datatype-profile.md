Base DataType Profile – Specification
====================================

Status: Draft  
Applies to: simplemodeling-lib  
Related article:
    Profile: Base DataType｜プロファイル：基本データ型

----------------------------------------------------------------------  
1. Purpose
----------------------------------------------------------------------  

This document defines the normative specification for
basic data types supported by simplemodeling-lib.

The purpose of this specification is to establish
a shared semantic vocabulary of basic data types
used in domain models, code generation, and validation,
while keeping the core library minimal and framework-independent.

This specification is intended for:

    - simplemodeling-lib maintainers
    - Cozy and code generation tooling
    - CNCF and SIE integration layers
    - AI agents assisting model-driven development

----------------------------------------------------------------------  
2. Scope
----------------------------------------------------------------------  

This specification covers:

    - Basic data types that require SimpleModeling-specific support
    - Semantic meaning and value constraints of those types
    - Construction and validation rules at the core library level

This specification does NOT cover:

    - Domain-specific business logic
    - Arithmetic operations or unit conversion
    - Serialization, persistence, or transport formats
    - Framework-specific integrations (HTTP, CLI, MCP, etc.)

----------------------------------------------------------------------  
3. Normative Reference
----------------------------------------------------------------------  

This specification is derived from the published article:

    "Profile: Base DataType｜プロファイル：基本データ型"

That article is considered the authoritative definition
of the Base DataType vocabulary and semantics.

In case of discrepancy, the article takes precedence,
and this specification must be updated accordingly.

----------------------------------------------------------------------  
4. General Principles
----------------------------------------------------------------------  

4.1 Standard Library Types

Data types already provided by Scala or Java standard libraries
MUST be used as-is and MUST NOT be wrapped.

Examples include, but are not limited to:

    - Int, Long, Boolean
    - String
    - BigInt, BigDecimal
    - java.time.*
    - java.net.URL, java.net.URI
    - java.util.UUID

4.2 SimpleModeling-Specific Types

SimpleModeling-specific data types MUST be introduced only when:

    - Semantic meaning cannot be expressed by primitive types alone, or
    - Explicit value range constraints are essential for modeling clarity

----------------------------------------------------------------------  
5. XML Schema Types
----------------------------------------------------------------------  

XML Schema data types referenced in the Base DataType Profile
exist solely as interoperability and value-range references.

Rules:

    - XML Schema types MUST NOT be reified as Scala types
    - No XML binding or serialization logic is provided in core
    - XML type information may appear only in documentation or Scaladoc

----------------------------------------------------------------------  
6. Construction and Validation Rules
----------------------------------------------------------------------  

All SimpleModeling-specific data types MUST follow these rules:

    - Constructors MUST be private
    - Public construction MUST be explicit
        (e.g. from, parse, unsafe variants)
    - Validation failures MUST NOT throw exceptions
    - Invalid construction MUST be represented explicitly
        (Option, Either, or Consequence)

The core library does not define a validation framework.
Validation semantics are limited to basic structural guarantees.

----------------------------------------------------------------------  
7. Category Overview
----------------------------------------------------------------------  

The Base DataType Profile defines the following categories
of SimpleModeling-supported data types:

    - Constrained Numeric Types
    - Semantic Text Types
    - Quantity and Measurement Types
    - Internationalization Types
    - I/O Related Types

These categories correspond to package-level organization
under org.simplemodeling.datatype.

----------------------------------------------------------------------  
8. Non-Goals
----------------------------------------------------------------------  

The following are explicitly out of scope for this specification
and for simplemodeling-lib:

    - Arithmetic or financial calculations
    - Unit conversion or normalization
    - Validation frameworks or rule engines
    - Logging, metrics, or observability
    - Serialization or transport semantics
    - Framework-specific helpers or utilities

----------------------------------------------------------------------  
9. Stability and Evolution
----------------------------------------------------------------------  

This specification is normative.

Once depended upon by:

    - Cozy-generated runtimes
    - CNCF components
    - SIE services
    - AI-assisted modeling workflows

changes MUST be conservative and backward-compatible.

Any extension or modification MUST be accompanied by:

    - An update to the Base DataType Profile article
    - A corresponding update to this specification
    - Explicit documentation of migration impact

----------------------------------------------------------------------  
10. Final Note
----------------------------------------------------------------------  

simplemodeling-lib defines a shared language.

The value of this specification lies not in feature richness,
but in semantic precision and long-term stability.

Be conservative.
Be explicit.
Design first.
