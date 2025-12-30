# Protocol Introspection — MCP get_manifest (1st Stage)

Purpose
----------------------------------------------------------------------
- get_manifest is a capability declaration for AI agents.
- It is NOT an execution contract.

Relationship to Protocol Core
----------------------------------------------------------------------
- Derived exclusively from ServiceDefinition / OperationDefinition.
- Definition-based, execution-free, deterministic.
- No independent introspection model exists in 1st stage.

Output Structure
----------------------------------------------------------------------
The conceptual structure includes:

- Service
- Operation
- Parameter
  - name
  - kind (argument / property / switch)
  - required
  - multiple
  - datatype (SimpleModeling primitive datatype name)

Runtime behavior is not included.

Constraints
----------------------------------------------------------------------
Definition-level predicate constraints MAY be included as declarative hints.
They are not execution guarantees and MUST NOT be treated as runtime validation.

Current field shape:

- constraints: { "opaque": true }

Datatype Representation
----------------------------------------------------------------------
- Datatype names are SimpleModeling primitive datatype names.
- Semantically aligned with XML Schema Datatypes (XSD).
- No namespace prefixes (no xsd:).
- Datatypes are semantic labels only.
- MCP clients MUST NOT assume parsing or validation semantics.

What get_manifest MUST include
----------------------------------------------------------------------
- Service name
- Operation list
- Operation name and description
- Parameter metadata (name, kind, required, multiple, datatype)

What get_manifest MUST NOT include
----------------------------------------------------------------------
- Execution behavior
- Error payload schemas
- Runtime metadata
- Transport / lifecycle / authorization constraints

Default Handling
----------------------------------------------------------------------
- Default.Value -> emit literal JSON value in \"default\"\n- Default.Empty -> emit { \"kind\": \"empty\" }\n- Default.Undefined -> omit \"default\" entirely\n\nMCP projection MUST NOT infer or materialize empty values.\nInterpretation belongs to the agent or client.

Stability Guarantees
----------------------------------------------------------------------
- Definition-based
- Deterministic output
- Stable across runtime changes

Relationship to OpenAPI
----------------------------------------------------------------------
- OpenAPI: execution interface
- MCP get_manifest: capability interface
- Both are projections derived from the same definitions
- Neither introduces new semantics

Example (Non-normative)
----------------------------------------------------------------------
{
  "service": {
    "name": "search"
  },
  "operations": [
    {
      "name": "query",
      "parameters": [
        {
          "name": "query",
          "kind": "argument",
          "datatype": "string",
          "required": true,
          "multiple": false
        }
      ]
    }
  ]
}
