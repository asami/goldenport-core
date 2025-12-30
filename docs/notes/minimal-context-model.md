Minimal Context Model Sketch
============================

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

This document sketches a **minimal Context model** to support
decision-making in goldenport core.

Its primary goal is to clarify:
- what Context is responsible for
- what Context is NOT responsible for
- how Context interacts with Conclusion

This is a design sketch, not a specification.
Concrete APIs are intentionally left undefined.

----------------------------------------------------------------------
2. Core Principle
----------------------------------------------------------------------

Context provides **conditions**, not **decisions**.

- Observation provides facts
- Context provides situation
- Conclusion performs decisions

Context MUST NOT encode business or operational decisions directly.

----------------------------------------------------------------------
3. Why Context Is Needed
----------------------------------------------------------------------

Certain decisions cannot be made from Observation alone.

Examples:
- DEV vs PROD handling
- CLI vs REST vs MCP behavior
- Interactive vs batch execution
- Strict vs permissive policy modes

These dimensions are orthogonal to domain facts
and therefore belong to Context.

----------------------------------------------------------------------
4. Minimal Context Dimensions
----------------------------------------------------------------------

A minimal Context model consists of orthogonal dimensions.

----------------------------------------------------------------------
4.1 EnvironmentContext
----------------------------------------------------------------------

Represents the execution environment.

Typical values:
- dev
- test
- prod

Purpose:
- influence severity interpretation
- influence handler selection

----------------------------------------------------------------------
4.2 ChannelContext
----------------------------------------------------------------------

Represents the interaction channel.

Typical values:
- cli
- rest
- mcp
- websocket

Purpose:
- influence presentation
- influence response strictness

----------------------------------------------------------------------
4.3 ExecutionModeContext
----------------------------------------------------------------------

Represents execution style.

Typical values:
- interactive
- batch
- scheduled

Purpose:
- influence retry or escalation strategy
- influence tolerance for failures

----------------------------------------------------------------------
4.4 PolicyContext
----------------------------------------------------------------------

Represents configurable policies.

Typical examples:
- strict vs lenient validation
- fail-fast vs accumulate
- user-defined error policy

Purpose:
- allow behavior variation without changing core semantics

----------------------------------------------------------------------
5. Context Composition
----------------------------------------------------------------------

Context is a **composite**, not a monolith.

A conceptual structure:

  Context =
    EnvironmentContext
  + ChannelContext
  + ExecutionModeContext
  + PolicyContext

Each sub-context is:
- independent
- optional
- replaceable

----------------------------------------------------------------------
6. Interaction with Conclusion
----------------------------------------------------------------------

Conclusion consumes Context to finalize decisions.

Typical flow:

1. Observations are collected
2. Severity is aggregated (context-independent)
3. Context is applied
4. Conclusion determines:
   - final severity
   - handler
   - strategy

Context MUST NOT:
- modify Observations
- re-interpret domain facts
- override Defect vs Domain Failure semantics

----------------------------------------------------------------------
7. Non-Goals
----------------------------------------------------------------------

This document does NOT:

- define inheritance hierarchies
- define storage or serialization formats
- define transport bindings
- define policy DSLs

Those belong to later design stages.

----------------------------------------------------------------------
8. Design Implication
----------------------------------------------------------------------

Future extensions may introduce:

- Context interfaces or traits
- Context builders or snapshots
- A decision layer consuming (Conclusion, Context)

Without altering:
- Observation structure
- Severity aggregation rule (max)
- WebCode vocabulary
- Throwable containment semantics

----------------------------------------------------------------------
9. Final Statement
----------------------------------------------------------------------

Context describes *where* and *how* execution happens.
It does not describe *what happened*.

By keeping Context minimal and orthogonal,
goldenport core preserves semantic clarity
while enabling flexible behavior.
