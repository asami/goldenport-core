# StateMachine Core Boundary

## Scope

`org.goldenport.statemachine` is the canonical core boundary for Phase-4 SM-01.

Core types are fixed as:

- `StateMachine[S, E]`
- `State`
- `Transition[S, E]`
- `Guard[S, E]`
- `Effect[S, E]`

CNCF and other runtimes MUST consume these core types and MUST NOT redefine
an alternative canonical model.

## Determinism

Transition selection order is normative:

1. Filter candidates by `(current state, event)`.
2. Sort by `priority` ascending.
3. For equal `priority`, preserve declaration order.
4. Select the first transition only.

No alternative strategy is allowed in core.

## Guard Semantics

Guard results are interpreted as:

- `Success(true)`: match
- `Success(false)`: non-match; continue scanning
- `Failure(...)`: guard evaluation failure; stop and return failure

Guard failures are explicit error values, not boolean fallbacks.

## Compatibility

`org.goldenport.model.statemachine` lifecycle enums (`PostStatus`, `Aliveness`)
are treated as adapter-level views.

Canonical transition semantics remain in `org.goldenport.statemachine`.
