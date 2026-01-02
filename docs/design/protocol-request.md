# Protocol Request

**Document:** Protocol Request  
**Category:** Design Rationale  
**Based on:** ValueBackedAbstractObject

---

## Intent

Represent a **protocol-level request object** that captures
*what is being requested* without implying *how it is executed*.

This idiom is used to model requests at system boundaries such as:

- CLI
- REST
- MCP
- RPC-like adapters

A ProtocolRequest:

- Is **purely descriptive**
- Contains **no execution semantics**
- Does **not** distinguish Command vs Query
- Is safe to serialize and transport

---

## Motivation

At protocol boundaries, it is often necessary to:

- Parse user or external input
- Resolve service / operation identifiers
- Validate basic structure
- Transfer the request across process or network boundaries

Embedding runtime concepts (e.g. Command, Query, ExecutionContext)
at this stage leads to premature coupling.

The ProtocolRequest idiom explicitly separates:

- **Protocol concern** (what was requested)
- **Runtime concern** (how it is handled or executed)

---

## Structure

ProtocolRequest is a **design boundary**, typically realized using the
**ValueBackedAbstractObject** structural idiom.

Its structure consists of:

- An abstract request object
- A single value object holding request data
- A default concrete instance
- Accessors exposed via a holder

---

## Canonical Form

```scala
abstract class OperationRequest
  extends OperationRequest.Core.Holder {

  def core: OperationRequest.Core
}

object OperationRequest {

  final case class Core(
    service: String,
    operation: String,
    params: List[String]
  )

  object Core {

    trait Holder {
      def core: Core

      def service: String = core.service
      def operation: String = core.operation
      def params: List[String] = core.params
    }
  }

  final case class Instance(
    core: Core
  ) extends OperationRequest

  def apply(
    service: String,
    operation: String,
    params: List[String]
  ): OperationRequest =
    Instance(
      Core(
        service,
        operation,
        params
      )
    )
}
```

---

## Semantic Rules

A ProtocolRequest:

- MUST NOT execute domain logic
- MUST NOT depend on runtime frameworks
- MUST be safe to serialize (JSON, etc.)
- SHOULD be immutable
- SHOULD be stable across protocol versions

---

## When to Use

Use the ProtocolRequest design boundary when:

- Handling CLI commands
- Accepting REST or MCP requests
- Bridging external input to internal systems
- Designing protocol or adapter layers

Typical locations:

- CLI application layers
- REST controllers / handlers
- MCP servers
- Protocol adapters

---

## When NOT to Use

Do NOT use ProtocolRequest for:

- Domain-level commands or queries
- Runtime execution objects
- Business logic carriers
- State-mutating operations

Instead, convert ProtocolRequest into:

- Command / Query (runtime layer)
- Domain-specific objects

---

## Relationship to Other Idioms

- **ValueBackedAbstractObject**  
ProtocolRequest is a semantic specialization of this structural idiom.

- **Command / Query (CNCF)**  
  Classification into Command or Query is a *runtime concern* and must
  occur **after** protocol handling.

---

## Known Uses

- `OperationRequest` (SIE CLI / REST / MCP)
- CLI argument parsing results
- REST request mapping objects

---

## Request Construction from Adapters

Requests constructed from adapter inputs (CLI, REST, MCP, etc.) are
**syntactically canonicalized at the ingress boundary**.

Canonicalization rules (argument, property, switch classification and
service/operation resolution) are defined in:

- `docs/design/cli-engine.md` — *Ingress Canonicalization (Syntactic Rules)*

This process is strictly **syntax-only**.
All semantic interpretation is deferred to `OperationDefinition`
and subsequent parameter resolution.

---

## Design Intent

The ProtocolRequest design boundary ensures that:

- Protocol layers remain thin and stable
- Runtime semantics are introduced deliberately
- System boundaries are explicit and reviewable
- AI-assisted tooling can reason about requests safely

By naming this pattern explicitly, design discussions and code reviews
can distinguish *protocol intent* from *execution behavior* with clarity.
