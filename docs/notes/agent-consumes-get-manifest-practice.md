# Practical Notes for MCP get_manifest Consumption
*(Notes)*

## Purpose

This document provides agent-facing, practical guidance for handling
`get_manifest` defaults, constraints, and tool schema generation.
It is non-normative and complements the conceptual note.

---

## Defaults

- Default.Value
  - Treat as an explicit default literal for tool schema generation.
  - Use the literal as-is in prompts or schema defaults.

- Default.Empty (monoid identity)
  - Do NOT materialize a literal value.
  - Represent as an explicit "empty" marker only when needed.
  - Prefer omission if the schema format does not support such markers.

- Default.Undefined
  - Omit any default field or hint.

Guideline:
- Do not invent defaults or reinterpret values.

---

## Constraints (Opaque)

- If constraints are present but opaque:
  - Treat them as non-actionable metadata.
  - Do NOT infer validation rules.
  - Do NOT reject inputs based on opaque constraints.

Guideline:
- Prefer conservative omission in tool schemas unless a constraint is
  explicitly understood by the agent.

---

## Tool Schema Generation

When generating tool schemas from `get_manifest`:

- Use datatype as a semantic label, not a validation rule.
- Use required/multiple flags to shape schema structure only.
- Avoid adding extra validation or type coercion.

---

## Prompt Scaffolding

- Explain defaults only when explicitly defined as Default.Value.
- Avoid implying runtime validation or constraint enforcement.
- Keep prompts aligned with the capability declaration model.

---

## Conversation Memory

- Store default literals as hints, not guarantees.
- Do not cache inferred constraints.
- Prefer refreshing schema knowledge if definitions change.
