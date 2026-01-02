Protocol Ingress — Syntactic Canonicalization
=============================================

Status
------
normative

----------------------------------------------------------------------
Scope
----------------------------------------------------------------------

This document defines the responsibility of **Protocol Ingress**.

Protocol Ingress is responsible for transforming external inputs
(e.g. CLI arguments, HTTP requests, MCP messages) into a canonical
`Request` representation.

This document specifies what Ingress MUST do and MUST NOT do.
It is normative and defines a strict responsibility boundary.


----------------------------------------------------------------------
Position in Protocol Architecture
----------------------------------------------------------------------

Protocol Ingress is positioned as follows:

    External Input
          ↓
       Ingress
          ↓
        Request
          ↓
   Protocol / Introspection / Execution

Ingress terminates at the creation of a canonical `Request`.
All semantic interpretation begins downstream of this boundary.

Ingress is a peer of protocol introspection, not a sub-function of it.
Both are connected by the `Request` object, which acts as the boundary
between input canonicalization and definition-driven processing.


----------------------------------------------------------------------
Ingress Responsibilities (MUST)
----------------------------------------------------------------------

Protocol Ingress MUST perform **syntactic canonicalization** only.

Specifically, Ingress MUST:

1. Identify the service name (if any) and the operation name.
2. Classify inputs into the following categories:
   - Argument
   - Property
   - Switch
3. Canonicalize positional inputs into syntactically named arguments
   (e.g. `param1`, `param2`, ...).
4. Produce a canonical `Request` object containing:
   - service
   - operation
   - arguments
   - properties
   - switches

Classification MAY consult available parameter definitions
(e.g. `ParameterDefinition`) but MUST remain purely syntactic.


----------------------------------------------------------------------
Ingress Non-Responsibilities (MUST NOT)
----------------------------------------------------------------------

Protocol Ingress MUST NOT perform semantic interpretation.

Ingress MUST NOT:

- Validate parameter values.
- Apply business rules.
- Perform type checking or coercion.
- Enforce constraints or required/optional semantics.
- Perform inference beyond direct name matching.
- Resolve overloads beyond immediate syntactic identification.

Ingress MUST NOT fail a request due to semantic invalidity.
Such validation belongs strictly to downstream layers.


----------------------------------------------------------------------
Canonical Request Shape
----------------------------------------------------------------------

The canonical `Request` produced by Ingress has the following shape:

- service: Option[String]
- operation: String
- arguments: List[Argument]
- properties: List[Property]
- switches: List[Switch]

The `Request` MUST be fully self-contained and independent
of the original input representation.


----------------------------------------------------------------------
Design Rationale
----------------------------------------------------------------------

Certain distinctions, most notably the classification of inputs into
arguments, properties, and switches, cannot be reliably reconstructed
after parsing.

If these distinctions are not made during Ingress,
downstream layers lose essential structural information.

Therefore, syntactic classification in Ingress is not an overreach;
it is a necessary precondition for correct protocol behavior.

This design preserves a strict separation:

- Ingress: structure and syntax
- Downstream layers: meaning and validation


----------------------------------------------------------------------
Normative References
----------------------------------------------------------------------

- ArgsIngressSpec (executable specification)

Related documents:

- protocol-core.md
- protocol-request.md


----------------------------------------------------------------------
Out of Scope
----------------------------------------------------------------------

This document does not cover:

- Transport-specific adapters (CLI, REST, MCP details).
- Execution, routing, or dispatch mechanisms.
- Error semantics beyond syntactic request formation.

----------------------------------------------------------------------
END OF DOCUMENT
----------------------------------------------------------------------
