======================================================================
Protocol Introspection & Projection Design (1st Stage)
CLI help / REST OpenAPI / MCP get_manifest
======================================================================

This document defines the first-stage design for protocol-based
introspection and projections derived from service and operation
definitions.

This design is normative for the first stage and serves as the canonical
reference for CLI help, REST OpenAPI generation, and MCP get_manifest.

----------------------------------------------------------------------
Scope
----------------------------------------------------------------------

This document covers:

- Definition-based introspection
- CLI help generation
- REST OpenAPI generation
- MCP get_manifest generation
- Error information policy (1st stage)

Out of scope:

- Execution
- Authorization
- Runtime error handling behavior
- Application-specific error payloads

----------------------------------------------------------------------
Design Principles
----------------------------------------------------------------------

- ServiceDefinition and OperationDefinition are the single source of truth.
- Introspection is definition-based and execution-free.
- All projections are derived from the same definition model.
- Projections may differ in format, but not in semantics.
- Error semantics are execution-time concerns and are not embedded in
  definitions.

----------------------------------------------------------------------
Definition and Introspection
----------------------------------------------------------------------

In the first stage, introspection is directly derived from definitions.

- No separate Introspection model is introduced.
- Introspection is treated as a type alias of definition structures.
- Projection-specific behavior is implemented via extension methods or
  type classes.

This approach is intentionally chosen to avoid premature abstraction.
If introspection later requires independent attributes, a dedicated
introspection model may be introduced.

----------------------------------------------------------------------
CLI Help Projection
----------------------------------------------------------------------

CLI help is generated directly from ServiceDefinition and
OperationDefinition.

CLI help MUST include:

- Service name
- Operation name
- Operation summary and description
- Parameter list
  - Name
  - Required / optional
  - Multiplicity
  - Datatype name
  - Short description

CLI help MUST NOT include:

- Execution behavior
- Error types or error payloads
- Runtime examples

Options are expressed in GNU-style long options:

- --parameter-name

Rationale:

- Long options are self-descriptive and readable.
- Short options (e.g. -p) are reserved for future extensions.
- The option namespace is extensible without ambiguity.

----------------------------------------------------------------------
REST OpenAPI Projection
----------------------------------------------------------------------

OpenAPI specifications are generated from the same definitions.

OpenAPI generation rules:

- Each operation is mapped to an endpoint.
- Parameters are mapped to query or path parameters.
- Datatypes are mapped to schema types.
- Required and multiplicity are preserved.

OpenAPI MUST include:

- Endpoint path
- HTTP method
- Parameters with datatype information
- Success response (200)

OpenAPI MUST NOT include by default:

- Error schemas
- Detailed error payload definitions

Error semantics MAY be referenced descriptively, but are not specified
structurally in the first stage.

Concrete datatype mappings for OpenAPI / JSON Schema are defined in
protocol-introspection-openapi.md.

Concrete MCP get_manifest structure and semantics are defined in
protocol-introspection-mcp.md.

----------------------------------------------------------------------
MCP get_manifest Projection
----------------------------------------------------------------------

MCP get_manifest exposes the capabilities of the service to AI agents.

get_manifest MUST include:

- Service name
- Operation list
- For each operation:
  - Name
  - Description
  - Parameters
    - Name
    - Required / optional
    - Multiplicity
    - Datatype name

get_manifest MUST NOT include:

- Execution behavior
- Error payload structures
- Runtime-specific metadata


The manifest is a capability declaration, not an execution contract.

Default and constraint representation rules are defined in
protocol-introspection-mcp.md.

----------------------------------------------------------------------
Datatype Representation in MCP get_manifest
----------------------------------------------------------------------

For a side-by-side comparison of protocol projections, see
protocol-introspection-comparison.md.

MCP get_manifest exposes datatypes as SimpleModeling primitive datatype
names.

Datatype names are aligned semantically with XML Schema Datatypes (XSD),
but MCP does NOT expose XML namespace prefixes (e.g. `xsd:`), schema
bindings, or external datatype identifiers.

Datatypes are provided as semantic labels only.
MCP clients and AI agents MUST NOT assume parsing, validation, or execution
semantics based on datatype names alone.

Protocol- or transport-specific datatype representations
(OpenAPI formats, JSON Schema types, XML schema bindings, etc.)
MUST be handled exclusively by projections and MUST NOT affect the
get_manifest output.

----------------------------------------------------------------------
Error Information Policy (1st Stage)
----------------------------------------------------------------------

This section defines the error information policy for first-stage
introspection and projections.

----------------------------------------------------------------------
Principles
----------------------------------------------------------------------

- Errors are execution-time semantics.
- Errors are NOT part of service or operation definitions.
- Definition-based projections do NOT describe concrete error payloads.
- Web/API-level error representations are unstable and may change.

Therefore, the protocol exposes only minimal and stable error information,
while treating detailed error data as opaque.

----------------------------------------------------------------------
Canonical Error Representation
----------------------------------------------------------------------

The canonical error representation is `Conclusion.Status`.

`Conclusion.Status` consists of:

- HTTP-level classification (`webCode`)
- Optional detail codes (`detailCodes`)
- Optional strategies (`strategies`)

Only the HTTP status code is considered stable and externally visible.
All other elements are opaque and application-specific.

----------------------------------------------------------------------
Field Semantics
----------------------------------------------------------------------

webCode
- Represents a high-level HTTP/Web classification.
- Used directly as HTTP status code in REST projections.
- Semantically stable, but implementation-specific.

detailCodes
- Represents internal or application-specific error classifications.
- Treated as opaque by all projections.
- Projections MUST NOT interpret or constrain their meaning.

strategies
- Represents declarative error handling or recovery hints.
- Treated as opaque and non-normative.
- Projections MUST NOT rely on or interpret these values.

----------------------------------------------------------------------
Projection Policy for Errors
----------------------------------------------------------------------

CLI help:
- Does NOT list error types or error payloads.
- Errors are reported only at execution time.

OpenAPI:
- Does NOT define error schemas by default.
- MAY reference the existence of error semantics descriptively.

MCP get_manifest:
- Does NOT include error definitions.
- Focuses exclusively on capability and input requirements.

----------------------------------------------------------------------
Default Error Catalog
----------------------------------------------------------------------

A default error catalog MAY be provided as documentation only.

The catalog lists possible error categories by name without defining
payload structure, HTTP mapping details, or message formats.

The catalog is informational and non-normative.

----------------------------------------------------------------------
Rationale
----------------------------------------------------------------------

Separating error classification from error representation ensures:

- Stable definition-based introspection
- Freedom to evolve Web/API error payloads
- Consistent CLI, REST, and MCP projections
- Isolation of application-specific concerns

Concrete error representation is intentionally deferred to
execution-time contexts.

----------------------------------------------------------------------
End of 1st Stage Design
----------------------------------------------------------------------
