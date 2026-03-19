# StateMachine Core Design (Phase 4 / SM-01)

status=journal
published_at=2026-03-19

## Context

CNCF Phase 4 requires a first-class state machine foundation.
Core primitives must be defined in simplemodeling-lib first,
then consumed by CNCF runtime via adapters.

This document fixes the SM-01 core-side design direction.

## Goals

- Define canonical core model for state/transition/guard/effect.
- Keep core pure (no I/O, no external runtime dependencies).
- Keep guard/action execution pluggable from CNCF side.
- Guarantee deterministic transition selection.

## Non-Goals

- Workflow engine implementation.
- Persistence architecture definition.
- CNCF runtime orchestration details.

## Layer Boundary

CML (DSL)
  -> AST
  -> core (simplemodeling-lib)
  -> CNCF runtime adapters

Core owns model and transition decision semantics.
CNCF owns binding and side-effect execution.

## Core Domain Model (Canonical)

Minimal canonical types:

- `StateMachineDef`
- `StateDef`
- `TransitionDef`
- `EventDef`
- `GuardExpr`
- `ActionRef`

Suggested shape:

```scala
sealed trait GuardExpr
object GuardExpr {
  final case class Ref(name: String) extends GuardExpr
  final case class Expression(expr: String) extends GuardExpr
}

final case class TransitionDef(
  to: String,
  event: String,
  guard: Option[GuardExpr],
  actions: Vector[ActionRef],
  priority: Int
)
```

## Determinism Rule (Normative)

Transition selection is fixed as:

1. Filter by `(currentState, event)`.
2. Evaluate guards.
3. Sort by priority ascending (smaller = higher).
4. For same priority, preserve declaration order.
5. Select first transition only.

No alternative selection strategy is allowed in core.

## Guard Evaluation Contract

Core should define an abstract evaluation contract.

```scala
trait GuardEvaluator[S, E] {
  def eval(guard: GuardExpr, state: S, event: E): Either[GuardError, Boolean]
}
```

Rules:

- `false` means normal non-match.
- parse/evaluation error means explicit failure (`Left`), not `false`.

## Binding Contracts (Adapter Boundary)

Core-level abstract contracts for named references:

```scala
trait GuardBindingResolver[S, E] {
  def resolve(name: String): Either[GuardError, Guard[S, E]]
}

trait ActionBindingResolver[S, E] {
  def resolve(name: String): Either[ActionError, ResolvedAction[S, E]]
}
```

Core must not know CNCF component/service/operation internals.

## Expression Engine Policy

Core model stores expression as plain string.
Expression engine choice is adapter-level policy.

Phase 4 baseline in CNCF: MVEL.

This keeps core free from hard dependency on a specific EL.

## Error Semantics

Core should distinguish:

- transition not found (normal non-match path)
- guard evaluation failure (configuration/execution error)
- binding failure (guard/action name not found or ambiguous)

These errors must remain explicit values, not implicit booleans.

## Compatibility with Existing Lifecycle Enums

`PostStatus` and `Aliveness` are treated as temporary lifecycle enums.
After core state machine integration:

- they may be mapped as adapters/views,
- but canonical transition semantics are owned by StateMachine core.

## Implementation Steps (SM-01)

1. Add/confirm canonical core model types.
2. Add deterministic planner contract and tests.
3. Add guard evaluation contract and error model.
4. Add resolver boundary contracts.
5. Add migration note for PostStatus/Aliveness adapter path.

## References

- `cloud-native-component-framework/docs/phase/phase-4.md`
- `cloud-native-component-framework/docs/phase/phase-4-checklist.md`
- `cloud-native-component-framework/docs/journal/2026/03/statemachine-dsl-execution-design.md`
