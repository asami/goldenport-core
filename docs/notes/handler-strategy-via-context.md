Handler / Strategy Decision via Conclusion + Context
===================================================

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document clarifies where and how **handler** and **strategy**
should be determined in the goldenport core error/result model.

It records an explicit design decision:

- handler and strategy MUST NOT be decided by Observation alone
- handler and strategy are determined by Conclusion in conjunction with Context

This document is a design note.
It defines responsibility boundaries, not concrete APIs.

----------------------------------------------------------------------
2. Background
----------------------------------------------------------------------

Earlier discussions identified a tension:

- Observation captures facts (what happened)
- handler / strategy describe reactions (what to do)

For example:

- The same validation failure may be:
  - Warning in CLI
  - Error in batch execution

- The same defect may be handled by:
  - Developer in DEV
  - Operator in PROD

This demonstrates that handler / strategy are **context-dependent**.

----------------------------------------------------------------------
3. Core Principle
----------------------------------------------------------------------

Observation records facts.
Context provides situation.
Conclusion performs decision.

handler and strategy are **decisions**, not facts.

Therefore:

- Observation MUST NOT decide handler or strategy
- Conclusion MAY carry handler / strategy
- The final values are determined using both Conclusion and Context

----------------------------------------------------------------------
4. Responsibility Separation
----------------------------------------------------------------------

The responsibilities are separated as follows:

----------------------------------------------------------------------
4.1 Observation
----------------------------------------------------------------------

Observation represents factual information only.

It MAY contain:
- cause kind
- raw severity (provisional)
- exception (opaque)
- timestamps and subjects

Observation MUST NOT:
- encode operational policy
- decide handler
- decide strategy

----------------------------------------------------------------------
4.2 Context
----------------------------------------------------------------------

Context represents situational information.

Typical context dimensions include:
- environment (dev / test / prod)
- execution channel (cli / rest / mcp)
- runtime mode (interactive / batch)
- policy configuration

Context contains **conditions**, not decisions.

----------------------------------------------------------------------
4.3 Conclusion
----------------------------------------------------------------------

Conclusion represents the **decision result**.

Using:
- aggregated observations
- context information

Conclusion determines:
- final severity
- handler
- strategy

Conclusion is the only place where:
- facts and context are combined
- operational meaning is fixed

----------------------------------------------------------------------
5. Relationship to Severity Aggregation
----------------------------------------------------------------------

Severity aggregation (max rule) is context-independent.

It defines how multiple severities are combined:

  Info + Warning + Error => Error

This aggregation happens **before** context-based decisions.

Context-based adjustments to severity, if any,
are performed after aggregation.

----------------------------------------------------------------------
6. Non-Goals
----------------------------------------------------------------------

This document does NOT:

- define concrete Context classes
- define handler or strategy enums
- define transport-specific behavior
- define retry or escalation logic

Those are future layers built on top of this rule.

----------------------------------------------------------------------
7. Design Implication
----------------------------------------------------------------------

Future extensions should introduce:

- a Context model (ExecutionContext, RequestContext, etc.)
- a decision layer (e.g. ConclusionResolver, DecisionPolicy)

Without modifying:
- Observation
- Severity aggregation rules
- Defect vs Domain Failure semantics

----------------------------------------------------------------------
8. Final Statement
----------------------------------------------------------------------

handler and strategy are not observed.
They are decided.

They are decided by Conclusion,
using Observation as evidence
and Context as condition.

This separation is foundational
to maintain semantic clarity and extensibility
in goldenport core.
