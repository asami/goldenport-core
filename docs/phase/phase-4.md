# Phase 4 — Type-safe equality and identity collection APIs

status=planned
execution_started=false

Purpose: Type-safe equality and identity collection APIs.
Progress: [Phase 4 checklist](phase-4-checklist.md).
This is the producer's own Phase; sm-workflow Phase 6.12 is its consumer.

Planned: 2026-11-09
Consumer: [sm-workflow 6.12-C](../../../sm-workflow/docs/phase/phase-6.12.md)
Coordination: [delivery dependencies](../../../sm-workflow/docs/phase/phase-6.12-dependencies.md)
Behavior: [three-stage strict Scala contract](../../../sm-workflow/docs/spec/scala-lint-profiles.md#three-stage-strict-scala-3-adoption)

## Scope and existing inputs

Deliver SLP-CORE: generic type-safe collection extensions with explicitly chosen
value equality or domain identity. Reuse Cats Eq and
`org.goldenport.id.Identified` / `Identifiable`; existing generic Eq instances
for StringDataType/IntDataType are inputs to qualification, not proof against
argument-driven widening. Final API names/imports are fixed by this delivery;
containsEq/containsId/findById/distinctById remain examples until accepted.

Core owns receiver element/ID type preservation, optional-ID semantics and
collection behavior. Model-specific ID semantics stay with SimpleModeling Model.
Do not add a competing Eq typeclass, a CNCF/Cozy/sm-workflow dependency, a lint
runtime or a global rewrite of existing collections. Distinct extension names
must avoid relying on extension dispatch to replace standard contains.

## Work and acceptance

The [Phase 4 checklist](phase-4-checklist.md) is the completion
ledger. Its existing delivery IDs and acceptance criteria are retained unchanged.

## Dependencies and first handoff

No new producer dependency is required to design or qualify Core APIs. The
consumer contract above supplies requirements, not a dependency on the consumer
application. Core returns its bounded accepted API before SimpleModeler, Cozy,
Model and AI consumers adopt it; Core does not wait for Phase 6.12 completion or
those consumers' whole Phases. Their integration feedback may identify a bounded
API issue without reopening closed Core Phase 3.

Existing Core phases/priorities, Goals, Sessions, accepted evidence and repair
counts remain unchanged. This plan authorizes no implementation, execution,
publication, commit or synchronization and marks no item complete.
