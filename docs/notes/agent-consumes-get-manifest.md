# How an Agent Consumes MCP `get_manifest`
*(Notes)*

## Purpose

This document explores how an AI agent **may** consume MCP `get_manifest`
information in practice.

This is **not a protocol requirement** and **not a normative design**.
It records reasoning patterns, usage models, and open questions related to
agent behavior.

The protocol itself remains **execution-free and AI-agnostic**.

---

## Positioning

- **Protocol Core** defines *what is exposed*.
- **MCP get_manifest** declares *capabilities*.
- **This note** explores *how an agent may interpret and use those capabilities*.

Agent behavior described here is:
- implementation-dependent
- model-dependent
- subject to evolution

---

## Typical Consumption Flow (Conceptual)

A common high-level flow for an AI agent:

get_manifest
  → capability graph
      → tool schema
          → prompt scaffolding
              → conversation memory integration

Each step is described below.

---

## Step 1: Capability Discovery

The agent retrieves `get_manifest` and interprets it as a **capability declaration**.

Key observations:
- Services and operations define *what can be done*
- Parameters define *what inputs are expected*
- No execution guarantees are implied

At this stage, the agent:
- builds a mental model of available actions
- does **not** attempt to call anything yet

---

## Step 2: Capability Graph Construction

The agent may construct an internal structure such as:

- Service → Operations
- Operation → Parameters
- Parameter → (kind, required, multiple, datatype)

This graph:
- is derived purely from definitions
- may be cached
- is independent of runtime state

---

## Step 3: Tool Schema Generation

From the capability graph, the agent may derive tool schemas suitable for
its execution environment (e.g., function calling, tool calling).

Important points:
- Datatypes are interpreted as **semantic labels**, not validation rules
- Required / optional flags guide schema shape
- No error schema is assumed

This step is **agent-specific** and not part of the protocol.

---

## Step 4: Prompt Scaffolding

The agent may incorporate manifest information into prompts, for example:

- Explaining available operations to itself
- Framing user intent in terms of known operations
- Selecting candidate operations for a task

The manifest acts as **contextual knowledge**, not instructions.

---

## Step 5: Conversation Memory Integration

An agent may integrate `get_manifest` with conversation memory:

- Remembering which capabilities exist
- Avoiding repeated introspection calls
- Associating operations with prior outcomes

Open considerations:
- When to refresh the manifest
- How to detect definition changes
- Whether per-session or global caching is appropriate

---

## What an Agent SHOULD NOT Assume

From `get_manifest`, an agent MUST NOT assume:

- Execution semantics
- Validation guarantees
- Error payload formats
- Transport-level constraints
- Authorization or lifecycle behavior

All of these are **outside the scope** of protocol introspection.

---

## Why This Is Not in `docs/design/`

This document is intentionally placed under `docs/notes/` because:

- Agent behavior is not a protocol boundary
- Best practices may evolve rapidly
- Multiple valid consumption strategies exist
- The protocol must remain AI-agnostic

Promoting this content to design would prematurely freeze assumptions.

---

## Open Questions

- How often should an agent refresh `get_manifest`?
- Should agents detect definition versioning explicitly?
- How much validation should an agent infer vs defer?
- How should multiple manifests be merged?

These questions are intentionally left open.

---

## Summary

`get_manifest` provides **capability information**, not execution rules.

This note captures one plausible and useful way an agent may consume that
information, while preserving the protocol’s core principles:

- definition-based
- execution-free
- projection-driven
- AI-agnostic

For practical guidelines on defaults, constraints, and tool schema generation,
see agent-consumes-get-manifest-practice.md.
