# stream-deck-prompts.md

======================================================================
Stream Deck Prompt Set — Rationale and Usage
======================================================================

This document explains why the current Stream Deck prompt set exists
and how it should be used during AI-assisted development.

This is NOT an AI instruction file.
This document is for the human operator (future self).

----------------------------------------------------------------------
Purpose
----------------------------------------------------------------------

The goal of these prompts is to:

- Stabilize architectural intent during long-running sessions
- Prevent quick hacks during demo-driven development
- Keep design and execution phases explicitly separated
- Make AI behavior predictable and recoverable

The demo itself is a means to *finalize architecture*, not a shortcut.

----------------------------------------------------------------------
Prompt Set Overview
----------------------------------------------------------------------

1. Bootstrap
   - Declares session-level rules and command semantics
   - Fixes interpretation drift at the beginning of a session
   - Used when starting fresh or after major confusion

2. @codex-current
   - L2 execution instruction only
   - Generates a self-contained Codex instruction
   - No explanations, no choices, no UI concepts
   - Used only after design decisions are settled

3. @patch-current
   - L1 patch-level directive
   - Applies a minimal local patch to the current file
   - Also covers the case where the previous patch was applied
     to the wrong file and must be re-applied here
   - No recovery logic, no redesign

4. Reality Check
   - Context injection and axis re-alignment
   - No execution, no patching
   - Confirms:
     - What is the current work (A)?
     - Are we still inside A’s completion criteria?
     - Are we avoiding “out of scope” escapes?

----------------------------------------------------------------------
Design Principles
----------------------------------------------------------------------

- Do not create new phases to escape difficulty
- Do not mark work complete unless the program runs
- Do not fragment problems into process theory
- Always return to the original work (A) after sub-tasks (B/C)

These prompts exist to enforce those principles mechanically.

----------------------------------------------------------------------
END OF DOCUMENT
----------------------------------------------------------------------
