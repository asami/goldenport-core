Chappie Command Catalog
======================

> **NOTE**  
> This document MUST be edited using Markdown heading units only.  
> Section-wide rewrites are FORBIDDEN unless explicitly approved.

## 1. Purpose

The purpose of this catalog is to:

- Prevent accidental full rewrites by AI tools
- Prevent scope expansion driven by naming or intuition
- Preserve architectural intent and working specifications
- Make AI behavior predictable, reviewable, and enforceable
- Minimize the amount of instruction text humans must write

AI tools MUST treat the commands defined here as **hard constraints**.


## 2. Command Forms

This catalog defines **two equivalent command forms**:

1. **Shorthand directives** (preferred, human-facing)
2. **Explicit AI-COMMAND blocks** (rare, exceptional use)

In normal development, **shorthand directives SHOULD be used**.


## 3. Shorthand Directives (Primary Interface)

Shorthand directives are **normative aliases** for explicit command sets.

They are written inline using `@command` notation.

Example:

```
@patch
CommandResolver の operation 省略解決を調整してください
```


## 4. Core Shorthand Directives

### @patch  (DEFAULT)

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- incremental-only
- rule-compliant
- no-scope-expansion
- format: text
```

#### Meaning

- Apply **minimal, local changes only**
- Preserve existing logic, structure, and responsibilities
- Do NOT refactor, redesign, or improve aesthetics
- Do NOT expand scope beyond existing intent

#### Notes

- This is the **default assumed mode**
- If no directive is provided, AI tools MUST behave as if `@patch` was specified
- `target` is inferred from immediate context (file name, class name, instruction text)


---

### @patch-context

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- incremental-only
- rule-compliant
- no-scope-expansion
- target: inferred-from-recent-context
- format: text
```

#### Meaning

- Apply **minimal, local changes only** (includes all guarantees of `@patch`)
- Explicitly allows **limited target inference** from the immediate conversation context
- Intended for use when the target file or document was clearly discussed
  in the previous one or two turns

#### Constraints

- Target inference is limited to **a single file or document**
- Inference MUST rely only on:
  - Explicit file names mentioned recently (e.g. `RULE.md`)
  - Direct references such as “this rule”, “the previous patch”, “that document”
- Code and documentation scopes MUST NOT be mixed
- If the target cannot be determined with high confidence,
  the AI MUST ask a clarification question before applying a patch

#### Notes

- `@patch-context` **includes `@patch` semantics by default**
- Using both `@patch` and `@patch-context` together is allowed but redundant
- Context-based inference is **forbidden unless this directive is explicitly present**

DEPRECATED:
- `@patch-context` is a deprecated alias of `@patch-current`.
- New workflows SHOULD use `@patch-current`.

#### Additional Authority (Explicit)

@patch-context MAY introduce new proposals ONLY under the following conditions:

1. Context Insufficiency
   - It is discovered that required assumptions or preconditions are missing or ambiguous
   - The AI MUST stop patch generation and ask explicit clarification questions
   - Speculative implementation is STRICTLY FORBIDDEN

2. Multi-file or Cross-cutting Changes
   - The required change clearly spans multiple files or responsibilities
   - The AI MUST NOT attempt to generate a direct patch
   - The AI SHOULD propose an appropriate `@codex-*` directive instead

---

### @patch-current  (PREFERRED)

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- incremental-only
- rule-compliant
- no-scope-expansion
- target: current-topic-via-oboe
- format: text
```

#### Meaning

- This directive is **L1 (patch-level)**.
- Apply a minimal, local patch to **the currently discussed target**.
- The patch is applied via **oboe**, using the current conversational focus
  as the authoritative patch target.
- This directive replaces `@patch-context` for clarity.

#### Constraints

- Target MUST be the single, currently discussed file or code region.
- The AI MUST NOT infer or expand scope beyond the current topic.
- If the current patch target cannot be identified with high confidence,
  the AI MUST stop and ask a clarification question.

#### Notes

- `@patch-current` includes all semantics of `@patch`.
- `@patch-context` is a **deprecated alias** of `@patch-current`.

---

### @repatch

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- incremental-only
- rule-compliant
- no-scope-expansion
- target: current-visible-file-only
- format: text
```

#### Meaning

- This directive is **L1 (patch-level)**.
- This directive is intended **only for re-applying a previously agreed patch**.
- The AI MUST assume that:
  - A patch was already agreed or generated immediately before, and
  - That patch was applied to an incorrect target **or** damaged the target.
- The AI MUST:
  - Re-apply the **same patch intent** to the **currently visible file**,
  - Treat the current file contents as the authoritative target state.
- The AI MUST NOT:
  - Introduce new changes, fixes, or interpretations,
  - Re-evaluate intent, infer context, or expand scope.
- If the patch intent cannot be identified with high confidence,
  the AI MUST stop and ask a clarification question.

#### Notes

- `@repatch` is intentionally context-free.
- This directive exists to enable **safe, deterministic recovery** from misapplied patches.

---

### @codex-spec-code

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- spec-first
- format: text
```

#### Meaning

- This directive is **L2-only**
- AI MUST generate a **Codex instruction document**
- The output MUST be suitable for direct copy-paste into Codex
- Working specifications MUST be described first, implementation steps second
- AI MUST NOT modify code or specifications directly

#### Scope Rule

- Any shorthand directive starting with `@codex-` is treated as **Codex instruction generation**
- `@codex-*` directives MUST NEVER apply patches or edit files
- Actual changes MUST be applied later using `@patch` or `@patch-context`

#### Typical Use

- Preparing a Codex instruction for spec-driven development
- Describing Spec → Code synchronization steps
- Delegating implementation work to Codex


### @handover

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- documentation-only
- format: markdown
```

#### Meaning

- Generate a standalone handover document summarizing the current stable state
- Intended for human readers (future self, collaborators)
- MUST NOT modify code or specifications
- The output MUST be reusable as-is to safely resume work later

#### Required Structure

The document MUST include the following sections:

1. Context Summary
2. Decisions (Final / Stable)
3. Deferred Items
4. Open Questions (if any)
5. Safe Resume Instructions

#### Constraints

- If required information is missing or ambiguous,
  the AI MUST explicitly state the uncertainty
  instead of producing a vague or speculative summary.
- The AI MUST NOT introduce new decisions or interpretations.

#### Typical Use

- End of a work session
- Context switching (different day, different focus)
- Preparing for Codex-assisted batch work

---

### @handoff (DEPRECATED)

This directive is deprecated and MUST NOT be used in new workflows.

Reason:
- The term "handoff" is semantically ambiguous and was observed to cause
  AI behavior overlap with `@handover`.

Use `@reanchor` instead.

---

### @reanchor

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- context-reanchor
- format: text
```

#### Meaning

- Re-anchor the current conversation state
- Intended to confirm shared understanding, not to produce artifacts

#### Constraints

- The AI MUST NOT generate a handover document
- The AI MUST NOT reorganize, summarize, or formalize decisions
- The AI MUST NOT introduce new structure or interpretation
- Output MUST be concise and conversational

#### Typical Use

- After receiving a handover document
- After a brief interruption
- To confirm “we are aligned on where we are”

#### Non-Goals

- Creating reusable documentation
- Producing session summaries
- Preparing future work instructions

---

### @codex-context

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- context-synthesis
- format: text
```

#### Meaning

- This directive is **L2-only**
- AI MUST generate a **Codex execution instruction** that can be
  **immediately pasted and executed** in Codex.
- The instruction describes the agreed changes, constraints, and scope
  discussed so far, and is **self-contained**.
- The output is NOT background context, explanation, or preparation text.
- If information required to safely generate executable instructions
  is missing, the AI MUST stop and ask clarification questions only.

DEPRECATED:
- `@codex-context` is a deprecated alias of `@codex-current`.

---

### @codex-current  (PREFERRED)

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- context-synthesis
- format: text
```

#### Meaning

- This directive is **L2-only**.
- AI MUST generate a **Codex execution instruction** that reflects
  the **current agreed state** of the conversation.
- The output MUST be a single, self-contained instruction that can be
  **immediately pasted and executed** in Codex.
- No background explanation or preparatory text is allowed.

#### Notes

- `@codex-current` replaces `@codex-context` to avoid ambiguity
  around the term “context”.
- `@codex-context` is a **deprecated alias** of `@codex-current`.

### @codex-code

#### Canonical Expansion

Equivalent to:

```
[AI-COMMAND]
- l2-instruction-only
- rule-compliant
- implementation-only
- format: text
```

#### Meaning

- AI MUST generate a fully self-contained Codex execution instruction.
- The instruction MUST be executable as-is when pasted into Codex.
- Surrounding conversational context MAY be used
  to supplement abbreviated or referential language in the instruction.
- Such supplementation MUST remain strictly within the intent
  explicitly stated in the instruction text itself.
- AI MUST NOT introduce new requirements, assumptions, constraints,
  or execution steps not directly implied by the instruction.
- If information required for safe execution is missing or ambiguous,
  AI MUST stop and ask clarification questions only,
  and MUST NOT generate an instruction.

## 5. Secondary Shorthand Directives

### @spec-only

#### Canonical Expansion

```
[AI-COMMAND]
- spec-only
- rule-compliant
- format: text
```

#### Meaning

- Only specification files may be modified
- Implementation MUST NOT be changed

#### Typical Use

- Writing or refining working specs
- Locking behavior before implementation


---

### @rewrite-ok  (DANGEROUS)

#### Canonical Expansion

```
[AI-COMMAND]
- rewrite-allowed
- rule-compliant
- format: text
```

#### Meaning

- Full rewrite is explicitly permitted

#### Warning

- This directive MUST be used sparingly
- Absence of this directive means rewrite is FORBIDDEN
- Reviewers SHOULD treat usage of this directive as a red flag


## 6. Explicit Command Blocks (Exceptional)

In rare cases where fine-grained control is required,
an explicit command block MAY be used.

Example:

```
[AI-COMMAND]
- incremental-only
- rule-compliant
- target: CommandResolver
- format: text
```

Explicit blocks SHOULD NOT be used in normal workflows.


## 7. Forbidden AI Behaviors (Global)

Regardless of directives, AI tools MUST NEVER:

- Rewrite entire files without `@rewrite-ok`
- Change public semantics without a working spec
- Expand scope beyond the stated responsibility
- Introduce new abstractions or layers unrequested
- Rename APIs for aesthetics or clarity
- Resolve TODOs or pending tests without instruction
- “Improve” design based on intuition


## 8. Priority Rules

When interpreting instructions, AI tools MUST follow this priority order:

1. RULE.md
2. This command catalog
3. Explicit shorthand directives
4. Working specifications (ScalaTest)
5. Existing code
6. AI intuition

When in doubt, AI tools MUST **do less, not more**.


## 9. Design Intent

This catalog ensures that AI tools act as:

- Controlled collaborators
- Deterministic editors
- Spec-driven assistants

NOT as:

- Autonomous designers
- Refactoring engines
- Architecture explorers

AI is a tool.
Design authority remains with humans and specifications.


## 10. Relationship to RULE.md

- RULE.md defines **what is allowed in code**
- This catalog defines **how AI may act on that code**

If a conflict exists:

RULE.md > Chappie Command Catalog > AI Intuition

## Related Workflow Document

Operational rules for tool selection, multi-file editing, and
failure handling are defined in:

- `docs/ai/chappie-workflow.md`

This command catalog defines command semantics only.

## 11. Quick Reference Table

| Directive        | Level | Purpose / Intent                              | Applies Changes |
|------------------+-------+-----------------------------------------------+-----------------|
| @patch           | L1    | Minimal local patch (default behavior)        | Yes             |
| @patch-context   | L1    | Minimal patch with explicit context inference (DEPRECATED alias of `@patch-current`) | Yes             |
| @patch-current   | L1    | Minimal patch to the currently discussed target (PREFERRED) | Yes             |
| @codex-context   | L2    | Generate executable Codex instruction (discussion-based, self-contained) (DEPRECATED alias of `@codex-current`) | No |
| @codex-current   | L2    | Generate executable Codex instruction reflecting current agreed state (PREFERRED) | No |
| @codex-code      | L2    | Generate executable Codex instruction (standalone, no context assumed)   | No |
| @spec-only       | L1    | Modify specifications only                    | Yes (spec only) |
| @rewrite-ok      | L1    | Explicitly allow full rewrite (dangerous)     | Yes             |
| @codex-spec-code | L2    | Generate Codex instruction (Spec → Code)      | No              |
| @codex-*         | L2    | Any Codex instruction shorthand               | No              |

Notes:
- **L1**: AI directly edits code or specifications under constraints
- **L2**: AI generates instructions only; no direct edits are permitted
- If no directive is provided, `@patch` is assumed
