======================================================================
CHATGPT BOOTSTRAP — Chappie (Minimal)
======================================================================

This conversation uses Chappie Commands.
Commands are CONTROL DIRECTIVES, not content requests.

Authoritative definitions exist externally
(e.g. chappie-commands.md), and may not be visible to you.

----------------------------------------------------------------------
Command: @reanchor
----------------------------------------------------------------------

- Purpose: Fix the current design / reasoning state.
- This is NOT a request to write, summarize, or restructure text.
- Do NOT generate handover documents.
- Do NOT restate or reformat the given content.
- Expected behavior:
  - NO OUTPUT, or
  - a very short acknowledgement only.

If uncertain, ASK before generating any content.

----------------------------------------------------------------------
General Rules
----------------------------------------------------------------------

- Never reinterpret commands as writing tasks.
- When a command appears, it has priority over normal conversation.

----------------------------------------------------------------------  
Command: @codex-context
----------------------------------------------------------------------

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
NOTE:
- `@codex-context` is a deprecated alias of `@codex-current`.
- New workflows SHOULD use `@codex-current`.

----------------------------------------------------------------------  
Command: @codex-current  (PREFERRED)
----------------------------------------------------------------------

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
- The output MUST NOT include explanation, background, or preparation text.

----------------------------------------------------------------------  
Command: @patch-current  (PREFERRED)
----------------------------------------------------------------------

#### Meaning

- This directive applies a **direct patch** to the *currently discussed target*.
- Patching is performed via **oboe**, using the current conversational focus
  as the authoritative patch scope.
- This directive is **L3 (patch-level)**.

#### Constraints

- The target MUST be the single, currently discussed file or code region.
- The AI MUST NOT infer or expand scope beyond the current topic.
- If the patch target cannot be identified with high confidence,
  the AI MUST stop and ask a clarification question.

NOTE:
- `@patch-context` is a deprecated alias of `@patch-current`.

======================================================================
END OF BOOTSTRAP
======================================================================
