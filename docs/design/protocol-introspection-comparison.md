# Protocol Introspection — Projection Comparison
*CLI Help / OpenAPI / MCP get_manifest*

## Purpose

This document provides a comparative view of how a **single canonical
OperationDefinition** is projected into multiple protocol-facing
representations.

The purpose is to demonstrate, concretely, that:

> **All projections are derived from the same protocol definitions and do not
> introduce new semantics.**

This document is explanatory and non-normative. It does not define new rules.

---

## Canonical Operation (Definition Level)

Assume the following canonical operation exists in the protocol core.

### Service
user

### Operation
createUser

### Parameters

| Name  | Kind      | Required | Multiple | Datatype |
|-------|-----------|----------|----------|----------|
| name  | argument  | yes      | no       | string   |
| age   | property  | no       | no       | integer  |
| admin | switch    | no       | no       | boolean  |

This definition, expressed via `ServiceDefinition`,
`OperationDefinition`, and `ParameterDefinition`,
is the **single source of truth** for all projections below.

---

## Projection Feature Comparison

### High-Level Comparison

| Aspect              | CLI Help        | OpenAPI                    | MCP get_manifest        |
|---------------------|-----------------|----------------------------|-------------------------|
| Primary audience    | Human           | HTTP client / tooling      | AI agent                |
| Purpose             | Usage guidance  | Execution interface        | Capability declaration  |
| Execution contract  | No              | Yes                        | No                      |
| Deterministic       | Yes             | Yes                        | Yes                     |
| Definition-based    | Yes             | Yes                        | Yes                     |

---

## Projection 1: CLI Help

### Representation

user createUser <name> [--age <integer>] [--admin]

Arguments:
  name        (required) string

Options:
  --age       integer
  --admin     boolean switch

Note:
  Arguments can be specified by position or by name (--name).

### Characteristics

- Optimized for human readability
- Supports positional and named arguments
- Datatypes are informational only
- No execution or validation semantics

### Conceptual Role

“How do I type this?”

---

## Projection 2: REST OpenAPI

### Representation (simplified)

{
  "paths": {
    "/user/createUser": {
      "post": {
        "operationId": "createUser",
        "requestBody": {
          "content": {
            "application/json": {
              "schema": {
                "type": "object",
                "required": ["name"],
                "properties": {
                  "name":  { "type": "string" },
                  "age":   { "type": "integer" },
                  "admin": { "type": "boolean" }
                }
              }
            }
          }
        },
        "responses": {
          "200": { "description": "OK" },
          "400": { "description": "Bad Request" },
          "500": { "description": "Server Error" }
        }
      }
    }
  }
}

### Characteristics

- Executable interface
- Formal JSON Schema validation
- Datatype mapping is projection-specific
- HTTP semantics introduced only in this projection

### Conceptual Role

“How do I call this?”

---

## Projection 3: MCP get_manifest

### Representation (simplified, non-normative)

{
  "services": [
    {
      "name": "user",
      "operations": [
        {
          "name": "createUser",
          "parameters": [
            {
              "name": "name",
              "kind": "argument",
              "required": true,
              "multiple": false,
              "datatype": "string"
            },
            {
              "name": "age",
              "kind": "property",
              "required": false,
              "multiple": false,
              "datatype": "integer"
            },
            {
              "name": "admin",
              "kind": "switch",
              "required": false,
              "multiple": false,
              "datatype": "boolean"
            }
          ]
        }
      ]
    }
  ]
}

### Characteristics

- Capability declaration only
- No execution or transport semantics
- Datatypes are SimpleModeling primitive names
- Intended for reasoning, not invocation

### Conceptual Role

“What can this system do?”

---

## Parameter Semantics Across Projections

| Aspect                | CLI Help      | OpenAPI            | MCP get_manifest |
|-----------------------|---------------|--------------------|------------------|
| Positional arguments  | Yes           | No                 | No               |
| Named arguments       | Yes           | Yes                | Yes              |
| Required flag         | Textual       | Formal schema      | Boolean          |
| Multiple flag         | Textual       | Array schema       | Boolean          |
| Datatype role         | Informational | Validation schema  | Semantic label   |

---

## Key Observations

1. **No projection modifies meaning**  
   All semantics originate in `OperationDefinition`.

2. **Each projection answers a different question**  
   - CLI Help → usage  
   - OpenAPI → execution  
   - MCP → capability  

3. **Datatype meaning is stable**  
   SimpleModeling primitive datatypes are the protocol truth.
   Projections may map them differently but never redefine them.

4. **Execution semantics are isolated**  
   Only OpenAPI introduces execution-level semantics.
   CLI Help and MCP remain execution-free by design.

---

## Conclusion

This comparison demonstrates that:

**Protocol projections are orthogonal views over a single canonical model.**

CLI Help, REST OpenAPI, and MCP get_manifest are not competing interfaces;
they are complementary projections, each optimized for a different audience,
and all derived from the same protocol core.
