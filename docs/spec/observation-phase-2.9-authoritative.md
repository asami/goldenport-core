# Observation Model — Phase 2.9 Authoritative Specification

This document records the authoritative semantics of Phase 2.9 Unified Observation.
The Scala implementation at `src/main/scala/org/goldenport/provisional/observation/Observation.scala`
is the single source of truth; all prose in this document is derived directly from that code.

## Core Observation Invariants

- **Observation** MUST capture a factual failure record described by `phenomenon`, `taxonomy`, `cause`, and a `timestamp` set at creation.
- **Phenomenon** is either `Phenomenon.Rejection` or `Phenomenon.Failure` and indicates the observable class of the event (rejection vs. failure).
- **Taxonomy** is a pair of independent enums (`Category` and `Symptom`) defining exactly what is factually wrong; observations MUST not rely on interpretation, severity, or handling to resolve their meaning.
- **Cause** is descriptor-driven (`org.goldenport.observation.Descriptor`); it provides descriptive facets such as messages, exceptions, constraints, or custom metadata but MUST NOT encode responsibility, judgment, or policy.
- **Timestamp** records when the failure was observed and MUST be set at observation construction (currently `Instant.now()`).

## Optional Diagnostic and Contextual Axes

- **Assessment** MAY record diagnostic severity or nature but MUST remain optional and not alter the factual meaning of the observation.
- **Occurrence** MAY describe the associated `Source`, `Channel`, and `Substrate` when available; it is purely descriptive of execution provenance.
- **Involvement** MAY capture `Subject`, `Agent`, and `Resource` participation without implying responsibility.
- **Origin** MAY identify the code location where the observation was emitted; it MUST remain interpretation-free and is intended for automatic population.
- **Environment** MAY describe `Mode`, `Location`, and `Platform` characteristics to contextualize the execution environment.

Each of these axes is optional and only enriches the observation with descriptive details; none MAY impose new semantics beyond the core invariants.

## Taxonomy Structure

- Taxonomy exists to describe *what is factually wrong* through two independent enums:
  - `Category` expresses the domain in which the problem was observed (`argument`, `property`, `configuration`, `resource`, `state`, `value`, `system`, `out-of-control`, `operation`).
  - `Symptom` describes the observable manifestation (`syntax-error`, `format-error`, `missing`, `redundant`, `unexpected`, `unsupported`, `not-found`, `unavailable`, `conflict`, `invalid-reference`, `illegal`, `invalid`, `corrupted`, `unreachable-reached`, `null-pointer`, `impossible-state`, `postcondition-violation`, `precondition-violation`, `invariant-violation`, `not-implemented`).
- Taxonomy entries are fixed constants with stable `name` and `value` metadata.
- Both axes MUST stay independent: documents or downstream layers MAY specialize their meaning but MUST treat Taxonomy as classification-only.

## Cause Semantics

- Cause encapsulates a `Descriptor` holding facets such as messages, exceptions, parameter names, and custom data.
- Cause SHOULD be constructed through helper factory methods (`Cause.apply`, `Cause.message`, `Cause.create`) to keep details descriptive and reusable.
- Cause MUST NOT attempt to encode interpretation, severity, policy, or handling decisions.

## Interaction and Provenance Details

- `Interaction` is a sealed abstract class representing concrete interaction forms (`Http`, `Database`, `FileSystem`, `Messaging`, `Opaque`).
- Interaction is shared across `Source.Detail` and `Channel.Detail` to avoid duplicating descriptive structures while keeping semantics contextualized by their enclosing axis.
- `Source` indicates where the triggering interaction originated; `Channel` describes how the interaction was performed; `Substrate` records where the condition manifested; `Origin` captures the code location; `Environment` describes the execution context.
- Each provenance axis is descriptive only; none may imply responsibility, judgment, or handling.

## Observations Must Not Encode

- Observations MUST NOT encode interpretation, responsibility, disposition, or handling strategy.
- Observations MUST NOT introduce severity, retryability, or policy in their core fields; such concerns belong exclusively to Conclusion / Consequence layers.
- Observations MUST NOT act as control-flow signals or business-rule enforcers; they are immutable factual snapshots.

This document exists to ensure all downstream documentation references a single canonical specification for Phase 2.9 observation semantics.
