# Phase 2.9 — Error Model Consolidation (Journal)

This journal entry records the current consolidated understanding of the
core error model, integrating legacy Fault-based design, the current
Consequence/Conclusion implementation, and Phase 2.9 discussions.

This document is provisional and intended to be merged into design
documents once the model stabilizes.

**Authoritative Specifications:** The canonical Phase 2.9 Observation and Conclusion models
are documented in `docs/spec/observation-phase-2.9-authoritative.md` and
`docs/spec/conclusion-phase-2.9-authoritative.md`, respectively. Treat those files as the definitive
reference points; this journal provides historical context aligned with them.

**Authoritative Specification:** The canonical Phase 2.9 observation semantics are now
captured in `docs/spec/observation-phase-2.9-authoritative.md`. Consider that document the
definitive reference; the text below preserves the historical journal conversation.

---
## Background

The goldenport-core error model has evolved from a rich Fault/Exception
hierarchy toward a value-based error representation using:

- Consequence
- Conclusion
- Observation

Phase 2.9 focuses on:
- clarifying missing semantics
- aligning legacy Fault concepts
- defining how errors are interpreted and handled downstream

---
## Consolidated Structural Model

Errors are represented and propagated using the following structure:

    Consequence
      └─ Conclusion
          ├─ Observation
          ├─ Interpretation
          └─ Disposition

- **Consequence**: execution result container (success or failure).
- **Conclusion**: program-carried error object.
- **Observation**: factual description of what happened.
- **Interpretation**: semantic judgment derived from context.
- **Disposition**: handling / presentation hints.

---
## Observation (Facts)

Observation records *what happened*, without deciding meaning or action.

### Observation Axes

Observation consists of the following axes:

- taxonomy   : what kind of event occurred
- cause      : immediate trigger
- source     : logical origin (e.g. in_memory, external)
- scope      : logical domain (optional)
- channel    : communication channel
- substrate  : execution substrate
- origin     : program location

Example:

    taxonomy  = resource.not_found
    cause     = lookup
    source    = in_memory
    channel   = in_memory
    substrate = jvm
    origin    = Method(UserAggregate#create)

### Notes

- taxonomy is descriptive, not prescriptive.
- the same Observation may lead to different interpretations.
- origin should be present whenever possible; Unknown is acceptable
  when automatic derivation is not available.

---
## Interpretation (Meaning)

Interpretation answers:

    “Given this Observation and the current context, what does this mean?”

Interpretation depends on:
- Observation
- Design by Contract (DbC)
- execution context
- caller/callee responsibility

### Interpretation Attributes

- kind:
    - domain_failure
    - defect
- expectation:
    - allowed
    - invariant_violation
- responsibility:
    - user
    - application_admin
    - system_admin
    - developer

Example:

    Observation:
      taxonomy = resource.not_found

    Interpretation (user lookup):
      kind = domain_failure
      responsibility = user

    Interpretation (internal invariant):
      kind = defect
      responsibility = developer

---
## Disposition (Handling Hints)

Disposition provides hints for downstream layers (CLI / UI / API).

It does not enforce behavior.

### Disposition Attributes

- user_action:
    - fix_input
    - retry_now
    - retry_later
    - escalation
- responsibility:
    - application_admin
    - system_admin
    - developer

Example:

    user_action    = escalation
    responsibility = application_admin

Meaning:
“This issue should first be handled at the application administration level.”

---
## Design by Contract (DbC)

DbC violations are represented as Observations and interpreted later.

### Observation Form

    taxonomy = contract_violation
    cause    = dbc_postcondition
    source   = in_memory
    origin   = Method(UserAggregate#create)

Additional structured details may include:

    contract_kind   = postcondition
    contract_target = UserAggregate#create

### Interpretation Rules

- Precondition violation:
    - may be domain_failure or defect (context dependent)
- Postcondition / invariant violation:
    - always defect

---
## Exceptions and Boundaries

- Exceptions are not the primary propagation mechanism.
- Internal logic uses Consequence / Conclusion.
- Exceptions are captured at boundaries and converted using:

    Conclusion.from(Throwable)

Exception messages are not assumed safe for direct UI exposure.

---
## Origin and Developer Experience

origin identifies where the Observation was created.

Recommended representation:

    sealed trait Origin
    case class Method(name: String) extends Origin
    case class Opaque(value: String) extends Origin

Scala 3 macros may be used to auto-derive Method origins,
enabling precise source-level diagnostics.

---
## Presentation and Projection

Conclusion is evaluated by presentation layers.

- PresentationContext derives defaults from ExecutionContext
- I18N / L10N are applied at presentation time
- org.goldenport.schema.Schema is used to validate message parameters

Note on terminology:

The existing term “Projection” is overloaded.
For error presentation, the concept aligns better with:

    ConclusionPresenter

---
## Current Status

- Structural model is mostly stable.
- Missing pieces:
    - standardized failXxx APIs
    - canonical error vocabulary catalog
    - clear mapping from legacy Fault classes
- This journal entry will be promoted to design documentation
  once Phase 2.9 converges.

## Provisional Implementation Validation (2026-01)

The provisional implementation under
`org.goldenport.provisional`
validated the following points through a runnable
end-to-end smoke test (Observation → Conclusion → Presentation → CLI).

### Confirmed Design Decisions

#### Conclusion-based propagation works

- Internal error propagation via `Conclusion` is viable.
- No exception handling is required inside the propagation path.
- Exceptions remain a boundary-only concern.

#### Presenter as an abstract class is appropriate

- `ConclusionPresenter` implemented as an `abstract class`
  with a template method pattern proved natural.
- The presenter cleanly separates:
  - semantic interpretation
  - presentation-oriented transformation
- No protocol-specific logic (CLI / HTTP) leaked into the presenter.

#### Observation / Interpretation / Disposition separation holds

- `Observation` contains only factual data.
- `Interpretation` is sufficient to derive:
  - severity
  - responsibility
- `Disposition` functions correctly as a non-binding hint:
  - e.g. `fix_input` reflected in presentation without enforcement.

#### Structured presentation model is necessary

- Direct string rendering inside the presenter is insufficient.
- Introducing a structured intermediate model
  (`PresentedConclusion`) enabled:
  - CLI rendering
  - exit code mapping
  - future protocol expansion

#### Renderer separation is effective

- CLI-specific concerns (exit code, formatting) are isolated
  in a renderer layer.
- The same presenter can theoretically serve
  HTTP / UI renderers without modification.

### Scope of Validation

- Validation was performed using a minimal smoke test
  (`SpecSmoke`) with ScalaTest.
- The implementation resides entirely under
  `org.goldenport.provisional` and is not considered stable.
- API shape, field names, and responsibilities may still change.

### Non-Decisions

The following remain intentionally undecided:

- Exact fields of `PresentedConclusion`
- Severity taxonomy and numeric exit code mapping
- Message catalog structure and I18N strategy
- HTTP / API error presentation model
- Promotion criteria from provisional to stable packages

This validation confirms architectural feasibility,
not final design commitment.

## Scala Mapping Targets (Phase 2.9)

This section enumerates design elements that have already been
discussed and stabilized to a level that allows *direct Scala mapping*.

The purpose of this section is:
- to serve as the **sole specification** for Codex-driven Scala drafts
- to eliminate guesswork and summarization
- to make compilation errors the primary feedback mechanism

No redesign, simplification, or interpretation is allowed
during the mapping phase.

---

### 1. Observation Model

Observation represents **facts only**.

It must not include:
- responsibility
- judgment
- countermeasures
- user-facing semantics

#### Required attributes

- taxonomy
- cause
  - category
  - detail
- source
- channel
- substrate
- origin (required, Unknown allowed)
- scope (optional)

#### Notes

- taxonomy is independent of responsibility and disposition
- channel and substrate are orthogonal axes
- the same Observation may yield different Interpretations

---

### 2. Interpretation Model

Interpretation represents **contextual meaning derived from Observation**.

It is derived from:
- Observation
- Design by Contract context
- call relationship / execution context

#### Required attributes

- kind
  - domain_failure
  - defect
- expectation
  - allowed
  - invariant_violation
- responsibility
  - user
  - application_admin
  - system_admin
  - developer

#### Rules

- the same Observation may result in different Interpretations
- DbC rules:
  - precondition violation:
    - domain_failure or defect (context-dependent)
  - postcondition / invariant violation:
    - defect (fixed)

---

### 3. Disposition Model

Disposition provides **non-binding handling hints**.

It must:
- never enforce behavior
- never override Interpretation
- act only as guidance

#### Required attributes

- user guidance
  - fix_input
  - retry_now
  - retry_later
  - escalation
  - no_action_required

#### Notes

- absence of user action is a **meaningful state**, not missing data
- Disposition does not imply responsibility

---

### 4. Conclusion Structure

Conclusion is the **unit of propagation** inside the system.

**Authoritative Conclusion Specification:** Definitive Conclusion semantics reside in
`docs/spec/conclusion-phase-2.9-authoritative.md`. Use that document for precise field definitions,
status semantics, causal chaining, and boundary helper behaviors.

#### Structure

- observation: Observation
- interpretation: Interpretation
- disposition: Disposition

#### Rules

- Conclusion is propagated instead of exceptions
- exceptions are boundary-only and must be converted into Conclusion

---

### 5. Presentation Responsibility

Presentation converts Conclusion into **final user-facing output**.

#### Presenter responsibilities

- derive severity from Interpretation
- resolve all user-facing messages into String
  - including I18N / L10N resolution
- apply PresentationContext
- produce a final, structured output

Presenter must not:
- leak protocol-specific logic
- defer message resolution to downstream layers

---

### 6. Presentation Output Model

The presentation output is **finalized**.

It must:
- contain resolved String messages
- retain structural meaning for downstream transports

#### Required attributes

- severity
- message (String, resolved)
- user guidance (explicit state, not Option)

---

### 7. PresentationContext

PresentationContext provides **presentation-time context only**.

#### Required attributes

- ExecutionContext reference
- locale
- time zone
- formatting policy

It must not:
- influence Observation or Interpretation
- be part of domain logic

---

### 8. Provisional Packaging Rules

All Scala drafts derived from this section must:

- reside under `org.goldenport.provisional`
- prioritize structural fidelity over API cleanliness
- allow breaking changes freely
- be considered non-stable

Promotion to stable packages is **explicitly out of scope**.

---

### 9. Mapping Policy for Codex

Codex must:

- treat this section and referenced journals as the only truth
- map each item mechanically to Scala types
- avoid redesign, optimization, or omission
- emit Scala drafts back into journal form for review

Compilation errors are expected and desirable.

---

**Authoritative Specification:** A finalized Phase 2.9 observation model now exists in
`docs/spec/observation-phase-2.9-authoritative.md`; the Scala drafts below are historical references.

### Scala Drafts (Codex)

The drafts below mirror the Phase 2.9 mapping targets without simplification. Each subsection lifts the journal text into provisional Scala definitions under the `org.goldenport.provisional.*` namespace so reviewers can debate the fidelity before any production code lands.

#### Observation

```scala
package org.goldenport.provisional.observation

sealed trait Origin
object Origin:
  case class Method(name: String) extends Origin
  case class Opaque(value: String) extends Origin
  case object Unknown extends Origin

sealed trait ScopeAxis
object ScopeAxis:
  case class Scope(value: String) extends ScopeAxis
  case object NoScope extends ScopeAxis

case class Cause(category: String, detail: String)

case class Observation(
  taxonomy: String,
  cause: Cause,
  source: String,
  channel: String,
  substrate: String,
  origin: Origin,
  scope: ScopeAxis
)
```

**Historical Note:** The canonical Phase 2.9 implementation now models `Cause` as a descriptor-based structure
and drives Observation semantics from `docs/spec/observation-phase-2.9-authoritative.md`. Treat the code above as an
archival sketch only.

#### Interpretation

```scala
package org.goldenport.provisional.interpretation

enum Kind:
  case domain_failure, defect

enum Expectation:
  case allowed, invariant_violation

enum Responsibility:
  case user, application_admin, system_admin, developer

enum ContractStage:
  case precondition, postcondition, invariant

sealed trait ContractContext
object ContractContext:
  case class Known(stage: ContractStage, detail: String) extends ContractContext
  case object Unknown extends ContractContext

case class Interpretation(
  kind: Kind,
  expectation: Expectation,
  responsibility: Responsibility,
  contractContext: ContractContext
)
```

#### Disposition

```scala
package org.goldenport.provisional.disposition

enum UserGuidance:
  case fix_input, retry_now, retry_later, escalation, no_action_required

enum EscalationTarget:
  case application_admin, system_admin, developer, not_applicable

case class Disposition(
  userGuidance: UserGuidance,
  escalationTarget: EscalationTarget
)
```

#### Conclusion

```scala
package org.goldenport.provisional.conclusion

import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.interpretation.Interpretation
import org.goldenport.provisional.disposition.Disposition

case class Conclusion(
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition
)
```

#### Presentation

##### Presenter

```scala
package org.goldenport.provisional.presentation

import org.goldenport.provisional.conclusion.Conclusion

abstract class ConclusionPresenter:
  def present(conclusion: Conclusion, ctx: PresentationContext): PresentationOutput
  protected def resolveMessage(conclusion: Conclusion, ctx: PresentationContext): String
  protected def deriveSeverity(conclusion: Conclusion): Severity
```

##### Output

```scala
package org.goldenport.provisional.presentation

import org.goldenport.provisional.disposition.UserGuidance

enum Severity:
  case INFO, WARN, ERROR

case class PresentationOutput(
  severity: Severity,
  message: String,
  userGuidance: UserGuidance
)
```

##### Context

```scala
package org.goldenport.provisional.presentation

import scala.concurrent.ExecutionContext
import java.time.ZoneId

sealed trait FormattingPolicy
object FormattingPolicy:
  case object Default extends FormattingPolicy
  case class Custom(pattern: String) extends FormattingPolicy

case class PresentationContext(
  executionContext: ExecutionContext,
  locale: String,
  timeZone: ZoneId,
  formattingPolicy: FormattingPolicy
)
```
