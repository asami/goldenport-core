# Tree / Record Definition — Semi-Structured vs Structured Trees

status=journal
published_at=2026-02-05

## Context

During the design of Tree and Record usage in simplemodeling-lib,
there was a risk of over-abstracting Tree as a central “IR hub”
between all representations (JSON, FS, Docker, REST, etc.).

This journal clarifies the final decision and definitions,
avoiding unnecessary architectural centralization.

## Definition

### Record — Semi-Structured Tree

**Record represents a semi-structured tree.**

- Tree-shaped data with no guaranteed semantics
- Node meaning is not fixed
- Leaf / directory distinction is ambiguous
- Structure may vary or be incomplete
- Suitable for:
  - JSON / YAML
  - REST payloads
  - Configuration
  - Draft or intermediate data

Record is intentionally *soft*.
It is a container for data, not an assertion of structure.

### Tree — Structured Tree

**Tree represents a structured tree with defined semantics.**

- Structure is explicit and trusted
- Leaf and directory are semantically distinct
- Order is meaningful
- PathName-based access is defined
- Operations such as `put`, `remove`, `walk`, and `map`
  are safe and well-defined

Tree is used when structural operations are the primary concern,
for example:

- File collections
- Docker input/output trees
- Execution-time structured data

## Relationship Between Record and Tree

Record and Tree are **not in a hierarchical relationship**.

They are **parallel representations chosen by use case**.

- Semi-structured tree → Record
- Structured, operable tree → Tree

Conversion between them is **optional and contextual**:

- Record → Tree  
  means *promoting* a semi-structured tree into a trusted structure
- Tree → Record  
  means *relaxing* a structured tree into a flexible representation

Neither conversion is mandatory, and Tree is not a universal hub.

## Rationale

This separation avoids:

- Treating all tree-shaped data as equally trustworthy
- Overloading Tree with responsibilities better suited to Record
- Centralizing all transformations through a single IR

Instead, it allows each representation to remain simple,
focused, and appropriate to its role.

## Summary

- Record is used for semi-structured tree-shaped data
- Tree is used for structured, semantically operable trees
- They are parallel tools, not layers
- Tree is not a mandatory IR hub

This definition provides a practical boundary
between flexibility and structural trust.
