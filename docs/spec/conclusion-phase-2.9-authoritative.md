# Conclusion Model — Phase 2.9 Authoritative Specification

This document records the canonical semantics of the Phase 2.9 Conclusion model.
The Scala implementation at `src/main/scala/org/goldenport/Conclusion.scala` is the single
source of truth; every statement below is derived directly from that code.

## Conclusion Definition

- Conclusion is an immutable value object comprising five fields: `status`, `observation`,
  `interpretation`, `disposition`, and an optional `previous` reference.
- `status` carries declarative metadata (`WebCode`, `detailCodes`, `strategies`) but MUST NOT
  trigger execution or handling logic; it is descriptive only.
- `observation` is the Phase 2.9 factual record defined in
  `org.goldenport.provisional.observation.Observation`; Conclusion contains no other core facts.
- `interpretation` and `disposition` represent semantic judgment and handling guidance, but MUST
  remain separate values that can be refined independently of `Observation`.
- `previous` MAY link to another Conclusion to express causal propagation; Conclusions without a
  predecessor represent the leaf of the chain.

## Status and WebCode

- `Status` is composed of a `WebCode` (HTTP-style code), optional `detailCodes`, and optional
  `ErrorStrategy` entries.
- `WebCode` lists fixed HTTP-equivalent constants (e.g. 200, 400, 500) that summarize high-level
  response intent but must not perform or imply handling decisions.
- `Status` MAY accumulate `ErrorStrategy` entries for downstream evaluators but MUST NOT apply them
  within the Conclusion model itself.

## Causal Chain Semantics

- `previous` forms the basis of the causal chain; `causes` flattens this chain in temporal order by
  walking ancestors and appending the current Conclusion without its `previous` pointer.
- `combine(a, b)` merges two `Conclusion` instances by concatenating `a.causes ++ b.causes`,
  reconstructing a combined chain, and updating the root observation severity to the greatest
  severity seen via `Severity.max`.
- Conclusion chains MUST remain immutable: merging produces new Conclusion instances that reuse
  the existing observations, interpretations, dispositions, and status values while preserving history.

## Boundary Helpers

- `from(Throwable)` and `fromThrowable` build Conclusions at boundaries by invoking `Taxonomy.from`
  and `Cause.from`, wrapping the result in a `Status` whose `WebCode` is `InternalError`, and
  carrying `Interpretation.from` / `Disposition.from` derived from the throwable.
- `simple(message)` constructs a minimal Conclusion with `Status.badRequest`, `Taxonomy.Argument/DomainValue`,
  `Cause.message`, and fixed interpretation/disposition helpers; it intentionally omits runtime metadata.
- `RAISE` throws a `ConsequenceException` that wraps `Consequence.Failure(this)` using the observation’s
  exception when available; `RAISEC` always throws the failure-wrapped `ConsequenceException` without
  relying on an exception instance.
- These helpers express boundary intent (propagate, rethrow, or wrap failures) but MUST NOT perform
  additional semantic work beyond the facts captured in the newly constructed Conclusion.

## Separation of Concerns

- Observation remains a factual-only record; Interpretation and Disposition record judgment and reaction
  hints; Status encodes declarative metadata. Each axis MUST be managed independently, and no field
  MAY imply responsibility for another axis.
- No Conclusion helper mutates Observation, Interpretation, or Disposition; combining Conclusions produces
  a new immutable structure.

## Conclusion MUST / MUST NOT

- Conclusions MUST capture the factual observation, interpretation, disposition, and status metadata
  without embedding execution logic.
- Conclusions MUST NOT encode severity propagation other than via their contained `Observation` and
  MUST NOT trigger handlers directly; such work belongs to downstream evaluators.
- Conclusions MAY link to previous Conclusions to describe propagation, but the chaining mechanism
  MUST preserve immutability and the order of occurrence.
