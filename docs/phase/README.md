# Core Development Phases

This directory records implementation plans and their completion checklists.

Planned consumer delivery, 2026-11-09:
[Phase 4: safe collection API](phase-4.md) / [checklist](phase-4-checklist.md)
records CORE-01–CORE-05, all OPEN. It reuses Cats Eq and existing identity
contracts; downstream adoption consumes a bounded accepted handoff, not Core
Phase 3 reopening or a whole consumer Phase completion dependency.

| Phase | Recorded status | Scope |
| --- | --- | --- |
| [4](phase-4.md) / [checklist](phase-4-checklist.md) | PLANNED | Type-safe equality and identity collection APIs; CORE-01–CORE-05 all OPEN |
| [3](phase-3.md) / [checklist](phase-3-checklist.md) | CLOSED | Record NonEmptyVector support and recursive null-attribute omission, preserving empty structures; Core review and full validation accepted; producer handoff for CNCF Phase 115 |
| [2.9](phase-2.9.md) / [checklist](phase-2.9-checklist.md) | Historical closure recorded | Error model realignment; original records are retained unchanged |

Phase 3 is a separate Core plan. Existing Workbench/CNCF-coordinated Core work
retains its own history and status; this index does not reaccept it or start a
new execution.
