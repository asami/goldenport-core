AI Collaboration Policy
-----------------------
This directory defines how AI assistants (e.g. Chappie, Codex) are used
in this project.

This policy is intended to share operational intent among developers and to ensure that AI assistants can understand, reason about, and operate on the codebase accurately with minimal cognitive load.

- Command files (e.g. chappie-commands.md) define executable instructions.
- Design and specification discussions are documented elsewhere.
- Design documents are written in English by default.
- Detailed specifications that involve Japanese-specific linguistic characteristics (e.g. full-width/half-width normalization, character variants, orthographic fluctuation) may be written in Japanese when precision requires it (e.g. spec/text-comparison-policy.md, spec/text-normalization-policy.md).

----------------------------------------------------------------------
Session Rules
----------------------------------------------------------------------

Session-level mandatory rules for AI collaboration are defined in:

- docs/ai/chappie-session.md

These rules MUST be applied at the beginning of every session
and take precedence over general workflows, commands, and
conversational defaults.
