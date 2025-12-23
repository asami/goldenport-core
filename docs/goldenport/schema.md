# Schema (Goldenport Design Reference)

Schema represents the **declarative structure of records**.

It defines *what is expected*, not *what is present*.

----------------------------------------------------------------------  
Purpose
----------------------------------------------------------------------

Schema exists to describe:

- record structure
- column definitions
- datatype expectations
- multiplicity rules
- validation constraints

Schema does NOT:
- hold values
- perform execution
- represent runtime state

----------------------------------------------------------------------  
Core Structure
----------------------------------------------------------------------

Goldenport Schema is minimal:

- Schema
  - columns: Vector[Column]

Everything else is derived from Column.

----------------------------------------------------------------------  
Declarative Nature
----------------------------------------------------------------------

Schema is purely declarative.

It represents:
- structural intent
- validation rules
- conversion expectations

Validation and conversion occur later.

----------------------------------------------------------------------  
Schema vs Record
----------------------------------------------------------------------

Important separation:

- Schema
  - structure
  - constraints
  - expectations

- Record
  - actual values
  - runtime data

Schema never contains values.

----------------------------------------------------------------------  
Column-Centric Design
----------------------------------------------------------------------

All meaningful behavior is attached to Column:

- datatype
- multiplicity
- constraints
- validation logic

Schema itself is a container, not a processor.

----------------------------------------------------------------------  
Validation Flow
----------------------------------------------------------------------

Typical flow:

1. Raw input (name → string)
2. Schema lookup
3. Column-based validation
4. Datatype conversion
5. Domain object construction

Schema enables this flow but does not execute it.

----------------------------------------------------------------------  
Inference and Generation
----------------------------------------------------------------------

Goldenport supports schema inference:

- from records
- from datasets
- from observed values

This is an optional capability,
not a core responsibility.

----------------------------------------------------------------------  
Scala 3 / SimpleModeling Interpretation
----------------------------------------------------------------------

In SimpleModeling core:

- Schema remains minimal
- Column is the primary abstraction
- CNCF may extend Column semantics
- core must not depend on runtime concerns

----------------------------------------------------------------------  
Status
----------------------------------------------------------------------

This document defines the **structural contract** of Schema.

All extensions must preserve:
- declarative purity
- value-free representation
