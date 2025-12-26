# CliEngine Design

## Purpose

CliEngine is an application-facing front class that bridges  
CLI-style arguments and semantic `OperationRequest` construction.

CliEngine is intentionally **not** an execution engine.  
Its responsibility ends at producing a fully validated  
`OperationRequest` instance.

---

## Design Position

CliEngine sits above `CliLogic` and below the application layer.

Application code interacts only with:

- `CliEngine`
- `OperationRequest` (domain-level request types)

CliEngine delegates all parsing and semantic interpretation  
to lower layers.

---

## Responsibility Boundaries

### CliEngine MUST

- Accept `ServiceDefinition` groups provided by the application
- Construct and own an internal `CliLogic` instance
- Convert CLI-style arguments into an `OperationRequest`
- Expose a simple, application-oriented API

### CliEngine MUST NOT

- Execute operations
- Perform side effects (IO, DB, network)
- Interpret parameter semantics
- Perform type conversion or validation logic

---

## API Design

CliEngine exposes a single primary method:

- `makeRequest(args: Array[String]): Consequence[OperationRequest]`

The name **`makeRequest`** is intentional:

- It indicates construction, not execution
- It preserves `execute` for future runtime responsibilities
- It aligns with `CliLogic.makeRequest` / `makeOperationRequest`

---

## Internal Structure

```
Application
  -> CliEngine.makeRequest
     -> CliLogic.makeRequest          (syntactic phase)
     -> CliLogic.makeOperationRequest (semantic phase)
     -> OperationRequest
```

CliEngine does not inspect or modify intermediate `Request` objects.  
It acts purely as a delegator and integrator.

---

## Application Usage Pattern

Applications define domain-level `OperationRequest` types and  
associate them with `OperationDefinition` objects.

Typical pattern:

- Define an `OperationRequest` case class
- Define its `OperationDefinition` in the companion object
- Register operations via `ServiceDefinition`
- Provide `ServiceDefinition` to `CliEngine.Specification`
- Call `makeRequest` with CLI-style arguments

From the application perspective:

- CLI arguments are just strings
- The result is a typed `OperationRequest`
- No protocol or CLI details leak upward

---

## Why `makeRequest` (not `execute`)

The name `makeRequest` is chosen to reserve conceptual space  
for future execution APIs.

Possible future additions include:

- `execute(OperationRequest)`
- `executeWithContext(Context, OperationRequest)`
- `run(OperationRequest)`

By separating request construction from execution,  
CliEngine remains lightweight and reusable across:

- CLI
- REST
- MCP
- Batch
- Test environments

---

## Design Stability

This design intentionally mirrors existing layering:

- `CliLogic` remains protocol-oriented
- `OperationDefinition` owns semantics
- `ParameterDefinition` / `ValueDomain` / `DataType` remain unchanged

CliEngine introduces no new interpretation logic  
and does not constrain future extensions.

---

## Status

- This document defines the initial stable shape of CliEngine
- The API is minimal by design
- Execution concerns are explicitly deferred
