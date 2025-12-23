# Column (Goldenport Design Reference)

Column represents the **semantic unit of structure and validation** in Schema.

While Schema is a container,
Column is where almost all meaningful behavior lives.

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Column exists to define:

- the name of a value
- its datatype
- its multiplicity
- its constraints
- how raw input is validated and converted

Column is the bridge between:
- declarative schema
- concrete runtime values

----------------------------------------------------------------------  
Core Responsibilities
----------------------------------------------------------------------

A Column encapsulates:

- identifier (name, aliases)
- datatype
- multiplicity (single, optional, multiple)
- constraints (size, range, pattern, etc.)

Column is responsible for:

- validating raw input
- converting raw values into typed values
- reporting semantic errors

----------------------------------------------------------------------  
Column vs Field
----------------------------------------------------------------------

Important distinction:

- Column
  - schema-level concept
  - declarative
  - value-free

- Field
  - record-level concept
  - holds actual values
  - runtime data

Goldenport intentionally separates these concepts.

----------------------------------------------------------------------  
Validation Role
----------------------------------------------------------------------

Column performs validation in cooperation with Datatype:

1. multiplicity validation
2. datatype validation
3. constraint validation
4. value conversion

Each step may contribute semantic errors.

----------------------------------------------------------------------  
Constraint Aggregation
----------------------------------------------------------------------

Constraints are attached to Column, not Schema.

Typical constraints include:

- size constraints
- range constraints
- pattern constraints
- domain-specific constraints

This allows Schema to remain minimal.

----------------------------------------------------------------------  
Extensibility
----------------------------------------------------------------------

Column is designed to be extensible:

- core provides minimal structure
- CNCF may enrich Column semantics
- runtime systems may attach additional metadata

However:

- core Column must remain runtime-agnostic
- execution logic must not leak into core

----------------------------------------------------------------------  
Scala 3 / SimpleModeling Interpretation
----------------------------------------------------------------------

In SimpleModeling core:

- Column should be a value-backed abstract object
- Constraints should be composable
- Validation returns Consequence

Column must not:
- depend on IO
- depend on async execution
- depend on framework-specific concepts

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **semantic role** of Column.

Implementations may vary,
but responsibility boundaries must remain intact.
