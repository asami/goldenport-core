# README Content Handover Notes

## Purpose

- Record content removed or linked out of README.md during entry-point cleanup.
- This is a maintenance memo, not a normative document.


## Moved / Linked Sections

### Error Handling Model

- Removed from README.md
- Canonical documents:
  - docs/spec/error-observation-integration.md
  - docs/notes/error-model-exploration.md
- Rationale:
  - README.md must remain a minimal entry point


### Framework / Application Integration (CNCF / SIE)

- Removed from README.md
- Canonical documents:
  - docs/design/
- Rationale:
  - Integration design details do not belong in the entry document


### Test Policy and Specification Style

- Removed from README.md
- Canonical documents:
  - docs/spec/test-policy.md
  - docs/spec/spec-style.md
- Rationale:
  - README.md must not restate policy
  - AGENTS.md and spec documents are authoritative


## Notes

- Keep README.md minimal and entry-focused.
- Do not move detailed policy or design text back into README.md.
- Add new normative content under docs and link from README.md.
