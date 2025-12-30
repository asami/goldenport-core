# Protocol Core

## Purpose and Scope

This document defines what “Protocol” means in this repository and serves as
its primary design entry point. All protocol-facing work MUST align with the
principles and boundaries defined here.

Protocol in this repository is the definition-based, execution-free boundary
that interprets requests and produces semantic results.

## What the Protocol IS

- A definition-based semantic boundary for request interpretation.
- A stable contract built on ServiceDefinition, OperationDefinition, and
  ParameterDefinition.
- Execution-free: it describes meaning and structure, not runtime behavior.
- Safe to serialize and transport across protocol adapters.

## What the Protocol IS NOT

- An execution engine or runtime policy layer.
- A CNCF/engine integration surface.
- A lifecycle or orchestration system.
- A place for application-specific error handling or transport rules.

## Core Invariants (MUST)

- Definitions are the single source of truth:
  ServiceDefinition, OperationDefinition, and ParameterDefinition.
- Protocol interpretation MUST be execution-free.
- ProtocolRequest boundary MUST remain distinct from runtime execution.
- Error semantics are execution-time concerns; protocol projections are
  definition-based.
- Protocol boundaries MUST remain serialization/transport safe.
- Semantic results MUST be normalized via Consequence/Conclusion.
- OperationRequest MUST be constructed only after datatype and constraint
  validation at the semantic boundary.
- Constraint opacity is a projection concern, not a protocol concept.

## Primitive Datatypes

SimpleModeling defines its own primitive datatypes as part of the protocol core.
These primitive datatypes are the only datatypes used in protocol definitions.

Their names and semantic definitions are aligned with XML Schema Datatypes (XSD),
but XSD is treated strictly as a reference model.
The protocol does not depend on XML Schema and never exposes XML namespace
prefixes such as `xsd:`.

All protocol definitions use SimpleModeling primitive datatype names
(e.g. `string`, `integer`, `dateTime`, `yearMonth`).
Domain-specific primitive datatypes (e.g. `age`) are expressed in the same way,
without any namespace or prefix distinction.

Protocol- or transport-specific datatype representations
(OpenAPI, JSON Schema, XML, MCP, etc.)
MUST be handled exclusively by projections and MUST NOT affect protocol
definitions.

## Core Boundaries

### Protocol-level request boundary

- Defines the protocol request idiom and structural boundary.
- See `docs/design/protocol-request.md`.

### Semantic boundary (OperationDefinition)

- Defines semantic interpretation and OperationRequest construction.
- See `docs/design/operation-definition.md`.

### Parameter resolution

- Defines extraction, normalization, and validation responsibilities.
- See `docs/design/parameter-resolution.md`.

### Result and error normalization

- Defines Consequence/Conclusion semantics.
- See `docs/design/consequence.md`.

### Cross-cutting i18n support

- Defines localization intent and roles.
- See `docs/design/i18n.md`.

## Projection / Introspection (Subsidiary)

Introspection and projection are derived from protocol definitions and are
subsidiary to the core protocol boundary.

- CLI help, REST OpenAPI, MCP get_manifest projections
- See `docs/design/protocol-introspection.md`.

## Subsystem-specific Adapters

- CLI adapter boundary: `docs/design/cli-engine.md`.

## How to Read Next

1. `docs/design/protocol-request.md`
2. `docs/design/operation-definition.md`
3. `docs/design/consequence.md`
4. `docs/design/parameter-resolution.md`
5. `docs/design/protocol-introspection.md`
6. `docs/design/cli-engine.md`
7. `docs/design/i18n.md`
