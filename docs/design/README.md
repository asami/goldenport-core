# Design Documents

This directory contains normative design documents defining boundaries,
contracts, and intent for core components.

Core Protocol Design:

- protocol-core.md
  Primary entry point for protocol boundaries and invariants.

- protocol-request.md
  Defines the protocol-level request boundary and canonical request structure.

- operation-definition.md
  Defines the semantic boundary for request interpretation and construction.

- consequence.md
  Defines result normalization semantics (Consequence/Conclusion).

Supporting and Cross-cutting:

- parameter-resolution.md
  Details parameter resolution responsibilities and value conversion flow.

- i18n.md
  Defines localization intent, roles, and responsibilities.

Projection / Introspection:

- protocol-introspection.md
  Defines projection rules for CLI help, REST OpenAPI, MCP get_manifest.

- protocol-introspection-openapi.md
  Defines OpenAPI / JSON Schema projection rules.

- protocol-introspection-mcp.md
  Defines MCP get_manifest projection rules.

- protocol-introspection-comparison.md
  Comparative view of CLI, OpenAPI, and MCP projections for the same operation.

Subsystem-specific:

- cli-engine.md
  Defines the CLI adapter boundary and request construction responsibilities.

Core Design Documents:

- operation-definition.md
  Defines the semantic boundary for request interpretation and OperationRequest construction.
- protocol-request.md
  Defines the protocol-level request boundary and canonical request structure.
- consequence.md
  Defines unified result semantics (exception boundary, validation, monadic composition).

Supporting and Subsystem Design Documents:

- parameter-resolution.md
  Details resolveParameter responsibilities and ValueDomain/DataType separation.
- i18n.md
  Defines message localization intent and role separation.
- cli-engine.md
  Defines the CliEngine boundary and API contract.
