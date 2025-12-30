# Error Model Exploration Notes

This document records exploratory notes and empirical findings
related to the error / observation model in simplemodeling-lib.


This is a non-normative, evolving document.
It intentionally avoids fixing final designs.

```
Note:
The normative distinction between Defect and Domain Failure
is defined in docs/notes/defect-vs-domain-failure.md.

This document intentionally explores design space
beyond that fixed semantic boundary.
```

The purpose is to:
- Preserve design context
- Capture usage-driven insights
- Avoid losing hard-earned experience from previous systems

----------------------------------------------------------------------
## 1. Background
----------------------------------------------------------------------

simplemodeling-lib is currently being used in:

- SIE CLI demo implementation
- Development of the CNCF configuration mechanism


At this stage, Conclusion / Consequence are intentionally lightweight.
They are expected to evolve based on concrete usage rather than
premature abstraction.

Note on roles:

Semantic Integration Engine (SIE) is an application,
not a framework.

It exercises the error / observation model indirectly
via the Cloud-Native Component Framework (CNCF).

Observations from SIE are treated as empirical input
for validation and refinement, not as normative design authority.

----------------------------------------------------------------------
## 2. Lessons from Goldenport (Historical Reference)
----------------------------------------------------------------------

The previous Goldenport framework provided a highly developed
error-handling model, particularly through Conclusion and Consequence.

Over time, a large set of utility methods emerged, such as:

- invalidPropertyFault
- missingPropertyFault
- valueDomainFault
- syntaxErrorFault
- illegalConfigurationDefect
- noReachDefect
- execute, run, using
- from(ParseResult)
- from(Try), from(Either)

These utilities were not created arbitrarily.
They were shaped by repeated real-world failures and operational needs.

### Key observation

These utilities were extremely productive for programmers:

- Domain failures could be expressed immediately
- No exceptions were required for expected errors
- Error intent was visible at call sites
- IDE auto-completion effectively exposed the error vocabulary

However, they also mixed multiple responsibilities:

- Observation (what happened)
- Interpretation (how severe / what category)
- Presentation (message, payload)
- Transport semantics (HTTP status, CLI behavior)

----------------------------------------------------------------------
## 3. Why Direct Reuse Is Not Appropriate
----------------------------------------------------------------------

While the Goldenport utilities were valuable, direct reuse in
simplemodeling-lib is problematic because:

- Error meaning is often context-dependent
- Mapping to WebCode / severity / reaction differs by:
  - CLI
  - Batch
  - API
  - Configuration loading
- Core library must remain framework-agnostic

In other words:

Goldenport utilities bundled fact + judgment + reaction
into a single call.

simplemodeling-lib aims to separate these concerns explicitly.

----------------------------------------------------------------------
## 4. Observation vs Interpretation
----------------------------------------------------------------------

An important distinction that emerged during discussion:

Observation:
  - What happened
  - Without judgment
  - Context-free

Interpretation:
  - How serious it is
  - How the system should react
  - Context-dependent

Example:

Observation.InvalidProperty(name = "age", value = -1)

This same observation may result in different conclusions:

- CLI tool: invalid input, show message, exit
- Config loader: illegal configuration, abort startup
- API: 400 Bad Request
- Internal pipeline: domain failure, propagate

This strongly suggests that error interpretation must be layered.

----------------------------------------------------------------------
## 5. Direction: ErrorSystem / ObservationSystem
----------------------------------------------------------------------

A possible direction under consideration:

- ObservationSystem
  - Responsible for constructing Observation instances
  - No interpretation or judgment

- ErrorSystem
  - Responsible for interpreting Observation into Conclusion
  - Context-aware
  - May handle from(Throwable) bridging

Both systems are candidates for an AbstractFactory-style design,
allowing:

- Core default behavior
- CNCF-specific overrides
- SIE-specific overrides
- Configurable selection at runtime

This direction is still exploratory.

----------------------------------------------------------------------
## 6. Current Minimal Usage (Config & CLI)
----------------------------------------------------------------------

At the current stage, Conclusion / Consequence are used primarily as:

- Structured return values
- Control flow without exceptions
- Internal signaling in Config loading and CLI execution

Typical usage patterns observed:

if (!exists(path))
  Consequence.failure(
    Conclusion(
      WebCode.InvalidInput,
      observation = Some(Observation.MissingProperty("configFile"))
    )
  )

This minimal usage is considered sufficient for now.

----------------------------------------------------------------------
## 7. Design Principles (Tentative)
----------------------------------------------------------------------

The following principles have emerged implicitly:

- Prefer explicit structure over convenience
- Preserve domain failure as values, not exceptions
- Delay interpretation until sufficient context is available
- Avoid premature commitment to Web / HTTP semantics
- Allow simplemodeling-lib to be usable standalone

These principles may be refined or revised.

----------------------------------------------------------------------
## 8. Open Questions
----------------------------------------------------------------------

- Which Goldenport utilities should be considered "standard"?
- Which ones clearly belong to higher-level profiles?
- How many ErrorContext layers are actually needed?
- Should ErrorSystem be purely functional or stateful?
- How much configurability is practical before complexity explodes?

These questions are intentionally left open.

----------------------------------------------------------------------
## 9. Status
----------------------------------------------------------------------

This document reflects the state of understanding during
the SIE CLI demo and CNCF configuration mechanism development phase.

It is expected to change.

----------------------------------------------------------------------
## Appendix A. Vocabulary Candidates from Goldenport Consequence
----------------------------------------------------------------------

This section extracts and classifies vocabulary that emerged organically
in Goldenport's Consequence implementation.

The goal is NOT to port APIs directly,
but to identify reusable semantic vocabulary
for simplemodeling-lib and higher-level profiles.

----------------------------------------------------------------------
### A.1 Vocabulary Categories
----------------------------------------------------------------------

Goldenport Consequence vocabulary falls into several distinct categories:

1. Control-flow helpers
2. Domain failure vocabulary
3. Configuration-specific vocabulary
4. Boundary / bridging utilities
5. Execution helpers
6. Transport-oriented shortcuts

These categories must NOT be mixed in simplemodeling core.

----------------------------------------------------------------------
### A.2 Core Domain Failure Vocabulary (High-Value)
----------------------------------------------------------------------

These represent *semantic error situations* repeatedly encountered
across systems.

They are strong candidates for Observation vocabulary
or ErrorSystem mappings.

- invalidArgument
- missingArgument
- emptyArgument
- tooManyArguments
- invalidProperty
- missingProperty
- invalidToken
- valueDomain
- syntaxError
- formatError
- resourceNotFound

Characteristics:
- Describe *what is wrong*, not *how to react*
- Appear across CLI, config, API, batch
- Highly reusable across domains

Likely destination:
- Observation vocabulary
- Possibly ObservationKind / Phenomenon subtypes

----------------------------------------------------------------------
### A.3 Defect / Invariant Violation Vocabulary (High-Value)
----------------------------------------------------------------------

These indicate system defects rather than user/domain failures.

- noReachDefect
- illegalStateDefect
- unmarshallingDefect
- illegalConfigurationDefect
- capacityOverflow

Characteristics:
- Represent "should not happen" conditions
- Often justify escalation or abort
- Useful for distinguishing fault vs defect

Likely destination:
- Observation with CauseKind = Defect
- ErrorSystem default mapping to severe Conclusion

----------------------------------------------------------------------
### A.4 Configuration-Specific Vocabulary (Context-Bound)
----------------------------------------------------------------------

These were highly useful but are clearly context-specific.

- config.invalidProperty
- config.missingProperty
- config.invalidToken
- config.valueDomain
- config.syntaxError
- config.illegalConfigurationDefect
- config.capacityOverflow

Characteristics:
- Same semantic shape as domain errors
- Different interpretation and severity
- Context-dependent (config loading phase)

Likely destination:
- CNCF Config ErrorSystem
- Not in simplemodeling-lib core
- Core provides only Observation primitives

----------------------------------------------------------------------
### A.5 Control-Flow Convenience Vocabulary (Mixed Value)
----------------------------------------------------------------------

These improved ergonomics significantly.

- successOrMissingProperty
- successOrInvalidArgument
- takeOrNoReachDefect
- orNoReachDefect
- getOrElse / orElse patterns
- recover / recoverWith

Characteristics:
- Improve readability
- Hide boilerplate
- Often encode assumptions implicitly

Likely destination:
- Profile libraries
- DSL layers
- Not core

Core should remain explicit.

----------------------------------------------------------------------
### A.6 Execution / Boundary Utilities (Out of Core)
----------------------------------------------------------------------

These handle exception boundaries or resource safety.

- execute
- run
- using
- runUsing
- from(Try)
- from(Either)
- from(ParseResult)

Characteristics:
- Bridge exception-based code
- Essential at system boundaries
- Context-sensitive semantics

Likely destination:
- ErrorSystem
- Boundary adapters
- CLI / framework layers

Core may define *interfaces*, not implementations.

----------------------------------------------------------------------
### A.7 Transport-Oriented Vocabulary (Explicitly Out of Core)
----------------------------------------------------------------------

These encode HTTP semantics directly.

- badRequest (400)
- unauthorized (401)
- forbidden (403)
- notFound (404)
- conflict (409)
- internalServerError (500)
- notImplemented (501)
- gatewayTimeout (504)

Characteristics:
- Transport-specific
- Not universally meaningful
- Act as shortcuts, not semantics

Likely destination:
- Web adapters
- API layers
- Never core

----------------------------------------------------------------------
### A.8 Summary: What to Preserve
----------------------------------------------------------------------

From Goldenport, the most valuable assets are:

- Semantic error *names*
- Distinction between:
    - Domain failure
    - Configuration error
    - System defect
- Proven ergonomics patterns (as inspiration)

What must NOT be preserved:

- Bundling observation + interpretation + transport
- Implicit context assumptions
- Direct HTTP mapping in core

----------------------------------------------------------------------
### A.9 Design Implication for simplemodeling-lib
----------------------------------------------------------------------

simplemodeling-lib should:

- Define semantic Observation vocabulary
- Provide minimal Conclusion / Consequence structure
- Allow ErrorSystem to map Observation → Conclusion
- Allow multiple ErrorSystem implementations
- Remain usable standalone with default mappings

Goldenport vocabulary serves as:
- Empirical evidence
- Naming inspiration
- Validation of semantic completeness

Not as a direct API template.

----------------------------------------------------------------------
## Appendix B. Vocabulary and Structural Insights from Goldenport Conclusion
----------------------------------------------------------------------

This section captures empirical design knowledge embedded in
Goldenport's Conclusion implementation.

Together with Appendix A (Consequence),
this forms the historical basis for designing
Observation / ErrorSystem / Conclusion in simplemodeling-lib.

----------------------------------------------------------------------
### B.1 Core Role of Conclusion (Empirical Definition)
----------------------------------------------------------------------

In Goldenport, Conclusion served as:

- A *judgment result* of an operation
- A container combining:
    - Classification (StatusCode)
    - Semantic detail (Faults)
    - Human-readable explanation (I18NMessage)
    - Diagnostic context (Trace)
    - Reaction hint (Strategy)

Crucially, Conclusion was **not merely an error**.
It represented *the system’s decision about what happened*.

This aligns closely with the intended role of Conclusion
in simplemodeling-lib.

----------------------------------------------------------------------
### B.2 Structural Vocabulary Embedded in Conclusion
----------------------------------------------------------------------

Goldenport Conclusion implicitly defines several important dimensions:

1. Classification
   - StatusCode
   - DetailCode
   - Web/API oriented, but abstractable

2. Semantic Detail
   - Fault
   - Faults
   - ArgumentFault, ResultFault, IoFault, Defect, etc.

3. Explanation
   - I18NMessage
   - Derived from:
       - Explicit message
       - Faults
       - ExceptionData
       - Status default

4. Context
   - Trace
   - Incidents
   - System location

5. Reaction Hint
   - Strategy
     - cache
     - reaction (input / retry / escalate)

These dimensions were *merged* into a single structure
for practical reasons.

simplemodeling-lib must *separate concerns*,
but should not lose these dimensions.

----------------------------------------------------------------------
### B.3 Fault Vocabulary as Semantic Gold
----------------------------------------------------------------------

The most valuable part of Goldenport Conclusion
is the **Fault vocabulary**.

Repeatedly used fault categories include:

- ArgumentFault
    - invalidArgument
    - missingArgument
    - emptyArgument
    - tooManyArguments

- PropertyFault
    - invalidProperty
    - missingProperty

- Token / Format Fault
    - invalidToken
    - syntaxError
    - formatError

- Domain Fault
    - valueDomain

- IO Fault
    - databaseIo
    - fileIo
    - networkIo
    - systemIo
    - subsystemIo

- Resource Fault
    - resourceNotFound

- Defect
    - noReach
    - illegalState
    - unmarshallingDefect
    - illegalConfigurationDefect

These faults encode *what went wrong*, not *how to respond*.

They are strong candidates for:

- Observation vocabulary
- Cause / Phenomenon taxonomy

----------------------------------------------------------------------
### B.4 Strategy as an Interpretation Layer (Important Insight)
----------------------------------------------------------------------

Goldenport introduced `Conclusion.Strategy`:

- cache strategy
- reaction strategy (input / retry / escalate)

This is a critical insight:

Reaction decisions are *derived*, not intrinsic.

Strategy was computed from Faults,
not hard-coded at call sites.

This supports the idea that:

- Observation should be judgment-free
- ErrorSystem should derive Conclusion
- Reaction hints belong to interpretation layers

In simplemodeling-lib:

- Strategy should NOT be mandatory
- Strategy derivation belongs to ErrorSystem defaults or profiles

----------------------------------------------------------------------
### B.5 Configuration Context as a Separate Interpretation Layer
----------------------------------------------------------------------

Goldenport had a dedicated `Conclusion.config` namespace.

This demonstrates a key empirical finding:

The *same semantic fault* requires
a different interpretation during configuration.

Example:

- missingProperty
    - Runtime: domain error
    - Config load: fatal configuration defect

Thus:

- Fault vocabulary is reusable
- Interpretation must be context-sensitive
- Configuration is a distinct phase/context

This maps naturally to:

- Multiple ErrorSystem implementations
- Phase-aware ErrorSystem selection

----------------------------------------------------------------------
### B.6 ExceptionData as Boundary Artifact
----------------------------------------------------------------------

Conclusion supported ExceptionData:

- HttpBody
- DataRecord
- JSON payloads

These are *transport / boundary artifacts*.

Key insight:

ExceptionData is useful,
but must not pollute core semantics.

In simplemodeling-lib:

- ExceptionData-like constructs should be optional
- Possibly defined in adapter layers
- Core should only define extension points

----------------------------------------------------------------------
### B.7 Anti-Patterns Identified
----------------------------------------------------------------------

Goldenport also reveals what should NOT be repeated:

- Mixing HTTP semantics directly into core
- Convenience methods that:
    - Encode interpretation implicitly
    - Hide context assumptions
- Conclusion factories that assume Web usage

These issues accumulated as the framework grew.

simplemodeling-lib has the opportunity
to avoid this from the beginning.

----------------------------------------------------------------------
### B.8 Integrated Vocabulary Map (Draft)
----------------------------------------------------------------------

Based on Goldenport experience, a layered vocabulary emerges:

Layer 1: Observation (Core, Stable)
- invalidArgument
- missingArgument
- invalidProperty
- missingProperty
- valueDomain
- syntaxError
- formatError
- resourceNotFound
- ioFailure(kind)
- defect(kind)

Layer 2: Interpretation (ErrorSystem)
- domainFailure
- configurationError
- systemDefect
- severity
- reactionHint

Layer 3: Transport / UI (Adapters)
- httpStatus
- cliExitCode
- payloadFormat

Only Layer 1 belongs in simplemodeling-lib core.

----------------------------------------------------------------------
### B.9 Design Implication Summary
----------------------------------------------------------------------

Goldenport Conclusion teaches:

- Rich error semantics are valuable
- Interpretation must be delayed
- Context matters more than convenience
- Vocabulary survives longer than APIs

simplemodeling-lib should:

- Preserve semantic vocabulary
- Separate observation from judgment
- Allow multiple interpretation strategies
- Remain usable without any framework

Goldenport serves as empirical proof,
not a template to copy.

