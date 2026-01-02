Chappie Editing Workflow
========================

This document defines the operational workflow for AI-assisted editing
in the ChatGPT Desktop + VSCode environment.

It complements `chappie-commands.md` by defining **how editing tools are chosen**
and **how failures are handled**, not command semantics themselves.

----------------------------------------------------------------------  
1. Scope
----------------------------------------------------------------------

This workflow applies when:

- Using ChatGPT Desktop with VSCode
- Performing AI-assisted edits on this repository
- Changes may involve one or more files

This document defines **operational rules**, not protocol or code semantics.

----------------------------------------------------------------------  
2. Tool Capabilities and Hard Limits
----------------------------------------------------------------------

The following tools have strict, non-negotiable limits:

### oboe.edit_file

- MUST be used for **single-file changes only**
- MUST NOT be used for multi-file changes
- In ChatGPT Desktop + VSCode, multi-file use is known to fail

### codex

- Repo-aware and capable of multi-file changes
- Suitable only for **mechanical, well-scoped edits**
- Requires explicit file lists and finalized text

### Manual (Human-in-the-loop)

- Always safe
- Required when tool reliability cannot be guaranteed
- Used as fallback and for high-risk edits

----------------------------------------------------------------------  
3. Multi-File Change Decision Flow
----------------------------------------------------------------------

When a change affects multiple files:

Step 0. Is this a multi-file change?
- If no → single-file rules apply
- If yes → continue

Step 1. Can codex be safely used?

Codex MAY be used ONLY IF ALL of the following are true:
- Target files are explicitly enumerated
- Changes are finalized (no design exploration)
- Changes are append-only or exact replacements
- No restructuring or refactoring is required
- No discretion or interpretation is required

If any condition is not met → codex MUST NOT be used.

----------------------------------------------------------------------  
4. Manual Multi-File Procedure (Per File)
----------------------------------------------------------------------

When codex is not suitable, each file MUST be handled independently
using one of the following cases.

### Case 1. Full File Replacement

Use when:
- The entire file content is replaced

Procedure:
1. AI outputs the complete final file content
2. Human replaces the file manually in VSCode

Tool usage:
- oboe: FORBIDDEN
- codex: FORBIDDEN

----------------------------------------------------------------------  
### Case 2. Append-Only Change

Use when:
- New content is appended to the end of the file

Procedure:
1. AI outputs:
   - Target file name
   - Explicit instruction to append
   - Final text to append
2. Human appends the text manually

Tool usage:
- oboe: FORBIDDEN
- codex: FORBIDDEN

----------------------------------------------------------------------  
### Case 3. Local In-File Modification (Clear and Isolated)

Use when:
- The change is localized
- The modification point is unambiguous
- Only ONE file is affected

Procedure:
- Use `oboe.edit_file` for that file only

Notes:
- Multi-file changes may still use oboe IF AND ONLY IF
  each file is edited independently in separate operations

----------------------------------------------------------------------  
### Case 4. Fallback (Case 3 Not Feasible or Failed)

Use when:
- The modification location is complex or ambiguous
- oboe.edit_file failed or is unreliable

Procedure:
1. AI MUST NOT apply edits
2. AI outputs:
   - File name
   - Description of modification intent
   - Precise location hints (function name, heading, etc.)
   - Proposed replacement text
   - Context diff (before / after) for human verification
3. Human performs the edit manually in VSCode

Tool usage:
- oboe: FORBIDDEN
- codex: FORBIDDEN

----------------------------------------------------------------------  
5. Non-Goals
----------------------------------------------------------------------

This workflow explicitly forbids:

- Automatic refactoring
- Design improvements by intuition
- Cross-file speculative edits
- Silent scope expansion

----------------------------------------------------------------------  
6. Relationship to chappie-commands.md
----------------------------------------------------------------------

- `chappie-commands.md` defines **what commands mean**
- This document defines **how tools are selected and applied**

If a conflict arises:
- RULE.md takes precedence
- Then this workflow
- Then command semantics

----------------------------------------------------------------------  
7. Design Principle
----------------------------------------------------------------------

This workflow exists to ensure that AI tools act as:
- Deterministic assistants
- Predictable editors
- Reviewable collaborators

NOT as autonomous agents.

Design authority remains with humans.

----------------------------------------------------------------------  
8. Output Formatting Rule (Copy-Paste Safety)
----------------------------------------------------------------------

When the AI outputs modification text intended for manual application
(e.g. replacement text, appended sections, revision snippets),
the following rule is mandatory:

- All such modification text MUST be enclosed in ````text fenced blocks.

Rationale:

- Prevent unintended Markdown rendering
- Preserve exact text boundaries for copy-paste
- Avoid accidental heading, list, or code interpretation
- Ensure deterministic manual application in VSCode

This rule applies to:
- Documentation edits
- Specification revisions
- Rule or policy text updates

Explanatory prose MAY be written outside text blocks,
but any content intended to be pasted into files MUST follow this rule.

----------------------------------------------------------------------  
9. Option Enumeration Rule (Disambiguation)
----------------------------------------------------------------------

When the AI presents multiple candidates, alternatives, or choices,
the following rule is mandatory:

- All options MUST be enumerated using alphanumeric identifiers
  (e.g. 1, 2, 3 or A, B, C).

Rationale:

- Prevent ambiguity when the user refers to a specific option
- Enable precise follow-up instructions (e.g. "choose option 2")
- Reduce conversational drift and misinterpretation

This rule applies to:
- Design alternatives
- Procedural choices
- Next-step proposals
- Any response where more than one selectable path is presented

Single-solution answers do not require enumeration.
