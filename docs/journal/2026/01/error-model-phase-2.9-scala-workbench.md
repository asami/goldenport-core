# Scala Drafts (v2) Workbench

This workbench rewrites the Phase 2.9 journals into pure Scala-definition drafts. Every `(TBD — Codex output)` placeholder below mirrors the canonical specification, and all code resides under `org.goldenport.provisional.*`. Compilation is intentionally loose; the goal is to provide reviewers with concrete Scala that matches the documented semantics prior to moving anything into `src/main/scala`.

---

## Observation

`Observation` contains facts only. Following the journal’s axes, the Scala draft keeps taxonomy, cause details, source, channel, substrate, origin (with Unknown allowed), and an explicit scope axis to capture optional scope semantics without using `Option`.

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

---

## Interpretation

Interpretation encodes semantic judgments (kind, expectation, responsibility) and preserves DbC context without mutating the original Observation.

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

---

## Disposition

Disposition carries handling hints only. It records explicit user guidance and escalation targets without importing responsibility semantics.

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

---

## Conclusion

**Authoritative Specification:** The canonical Conclusion structure lives in
`docs/spec/conclusion-phase-2.9-authoritative.md`. The code snippets below are historical
Codex drafts; refer to the authoritative spec for definitive behavior.

Conclusion transports the Observation / Interpretation / Disposition bundle through program layers.

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

---

## Presentation

Presentation layers resolve conclusions into user-facing output. The presenter remains abstract (template method) and resolves all messages into strings before emitting a structured output that retains severity and guidance.

### Presenter

```scala
package org.goldenport.provisional.presentation

import org.goldenport.provisional.conclusion.Conclusion

abstract class ConclusionPresenter:
  final def present(conclusion: Conclusion, ctx: PresentationContext): PresentationOutput =
    val severity = deriveSeverity(conclusion)
    val message = resolveMessage(conclusion, ctx)
    val guidance = conclusion.disposition.userGuidance
    PresentationOutput(severity, message, guidance)

  protected def deriveSeverity(conclusion: Conclusion): Severity
  protected def resolveMessage(conclusion: Conclusion, ctx: PresentationContext): String
```

### Output

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

### Context

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
