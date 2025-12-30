======================================================================
Protocol Introspection — OpenAPI / JSON Schema Projection (1st Stage)
======================================================================

1. Purpose
----------------------------------------------------------------------
Define OpenAPI / JSON Schema projection rules derived from protocol
definitions. This is a projection-specific design, not protocol core.

2. Scope
----------------------------------------------------------------------
Included:
- OpenAPI generation
- JSON Schema usage inside OpenAPI
- Datatype mapping
- 1st stage constraint policy

Excluded:
- Runtime behavior
- Execution semantics
- Authorization
- Application-specific extensions

3. Design Principles
----------------------------------------------------------------------
- ServiceDefinition and OperationDefinition are the single source of truth.
- OpenAPI is a derived view, not an execution contract.
- Datatype names originate from SimpleModeling primitive datatypes.
- Projection MUST NOT introduce new semantics.

4. Relationship to Protocol Definitions
----------------------------------------------------------------------
- OpenAPI schemas are mechanically generated from definitions.
- Definitions never depend on OpenAPI or JSON Schema.
- All OpenAPI-specific details live exclusively in this projection.

5. Primitive Datatype Mapping
----------------------------------------------------------------------
Input vocabulary is SimpleModeling primitive datatype names. Mapping targets
OpenAPI v3.x / JSON Schema. XML namespace prefixes (e.g. xsd:) are NEVER used.

SimpleModeling datatype | OpenAPI type | format     | notes
----------------------- | ----------- | ---------- | -----------------------
string                 | string      | (none)     | default
integer                | integer     | int64      | canonical integer
nonNegativeInteger     | integer     | int64      | minimum: 0
positiveInteger        | integer     | int64      | minimum: 1
dateTime               | string      | date-time  | ISO-8601
yearMonth              | string      | (none)     | ISO-8601 YYYY-MM
age                    | string      | (none)     | domain primitive example

These mappings are projection rules only. They do NOT redefine protocol
datatype semantics.

6. Constraint Mapping Policy (1st Stage)
----------------------------------------------------------------------
- Only simple numeric constraints (minimum, exclusiveMinimum) MAY be emitted.
- Constraint emission is conservative and optional.
- No complex validation logic is embedded.

Default and Constraint Metadata
----------------------------------------------------------------------
Default mapping (ParameterDefinition.default):

- Default.Value(v)\n  -> emit JSON Schema/OpenAPI \"default\" with a JSON literal\n     (string/number/boolean); fallback to string when not representable.\n\n- Default.Empty\n  -> DO NOT emit standard \"default\".\n  -> emit extension: x-default-kind: \"empty\".\n\n- Default.Undefined\n  -> emit nothing.\n\nConstraints mapping (current stage):\n\n- If constraints are empty: emit nothing.\n- If constraints exist but are not concretely mapped yet:\n  emit extension: x-constraints: \"opaque\".\n\nThese metadata are projection-only and do not mandate runtime behavior.\nMCP uses a different representation for Default.Empty; OpenAPI uses x-*.\n+
7. What Is Explicitly Out of Scope
----------------------------------------------------------------------
- Error schemas
- oneOf / anyOf / allOf
- Polymorphism
- Vendor extensions
- Custom formats

8. Rationale
----------------------------------------------------------------------
Datatype mapping is projection-specific because OpenAPI is a derived view.
protocol-core.md avoids OpenAPI detail to preserve protocol purity. MCP and
CLI use different datatype representations, so mapping belongs here.

9. End of 1st Stage Design
----------------------------------------------------------------------
End of 1st Stage Design
----------------------------------------------------------------------
