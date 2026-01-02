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

---

## Ingress Canonicalization (Syntactic Rules)

This section defines the **syntactic canonicalization rules** applied during
the `CliLogic.makeRequest` phase, commonly referred to as *Ingress*.

Ingress is responsible for converting adapter-level inputs
(CLI arguments, REST requests, etc.)
into a canonical, **purely syntactic** `Request` representation.

No semantic interpretation is performed at this stage.

---

### Scope and Intent

Ingress canonicalization:

- Operates **only on syntax**
- Produces a `Request` object containing:
  - operation name
  - arguments
  - properties
  - switches
- Does **not**:
  - validate parameter semantics
  - resolve types
  - apply defaults
  - interpret domain meaning

All semantic interpretation is deferred to `OperationDefinition`
and parameter resolution logic.

By default, Ingress operates with an *unresolved service context*.
That is, it assumes only a `ServiceDefinitionGroup` is known,
and no specific `ServiceDefinition` or `OperationDefinition`
is fixed in advance.

This default corresponds to typical CLI usage and is the primary
responsibility of `ArgsIngress`.

---

### Operation and Service Resolution (CLI)

CLI invocation supports the following input forms:

1. `serviceName operationName arg1 arg2 ...`
2. `operationName arg1 arg2 ...`

Resolution rules:

- If `args(0)` matches a known `ServiceDefinition` name:
  - `args(1)` is treated as an operation name within that service
- Otherwise:
  - `args(0)` is treated as an operation name
  - the operation is searched across all registered services

This resolution is **syntactic only** and does not validate
whether arguments are semantically correct.

These rules are evaluated with the following priority:

1. Service name resolution (if explicit)
2. Operation name resolution
3. Argument / property / switch classification

Ingress must complete service and operation identification
*before* parameter classification, as later stages depend on
a stable operation context.

---

### Argument, Property, and Switch Canonicalization

Ingress classifies CLI tokens into the following categories:

#### Positional Arguments

- Tokens without a `--` prefix are treated as positional arguments
- They are recorded as `Argument` entries in the `Request`
- Naming is syntactic and positional:
  - e.g. `param1`, `param2`, ...
- No semantic meaning is inferred from position

#### Named Properties

- Tokens in `--name value` form are treated as named inputs
- By default, these are recorded as `Property` entries

#### Switches

- Tokens in `--flag` form (without a following value)
  are treated as `Switch` entries

---

### Interaction with OperationDefinition

Ingress may consult `OperationDefinition` **only to classify syntax**.

Specifically:

- If a `ParameterDefinition` with kind `Argument` exists for a given name:
  - `--name value` is recorded as an `Argument`
- If a `ParameterDefinition` with kind `Property` exists:
  - `--name value` is recorded as a `Property`
- If a `ParameterDefinition` with kind `Switch` exists:
  - `--flag` is recorded as a `Switch`

Ingress **must not**:

- interpret parameter meaning
- enforce cardinality
- validate value domains
- apply default values

These responsibilities belong exclusively to semantic resolution.

Ingress may therefore require access to one of the following,
depending on application configuration:

- `ServiceDefinitionGroup` (default, service unresolved)
- `ServiceDefinition` (service fixed, operation unresolved)
- `OperationDefinition` (operation fixed)

The default CLI path uses `ServiceDefinitionGroup`,
ensuring that Ingress remains reusable and application-agnostic.

---

### Canonicalization Invariants

After Ingress processing:

- `Request` contains a **fully normalized syntactic form**
- CLI- and REST-originated requests share the same shape
- Semantic layers may rely on:
  - correct separation of arguments / properties / switches
  - stable operation and service resolution
  - absence of domain interpretation at the syntactic boundary

This guarantees adapter symmetry and preserves a strict
syntax–semantics separation.