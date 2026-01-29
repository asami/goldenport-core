Journal: Error Concept Article vs Phase 2.9 Observation Model
============================================================

date: 2026-01-25
context: Consequence / Conclusion / Observation Phase 2.9 redesign
reference:
- SimpleModeling.org「Error Concept｜エラー・コンセプト」（2025-09-29）

Purpose
-------

This journal records the comparison between the previously published *Error Concept*
article and the current Phase 2.9 redesign of Observation / Conclusion / Consequence,
focusing on conceptual alignment and evolution.

The goal is traceability of design intent, not revision or correction of the article.

Overall Assessment
------------------

- No conceptual contradiction was found.
- The Phase 2.9 model represents a structural and responsibility-level refinement,
  not a change in meaning.
- The article corresponds to a conceptual / analytical model, while Phase 2.9
  represents an execution-oriented, AI- and DSL-friendly model.

Key Evolution Points
--------------------

1. Center of Gravity Shift

Article (2025):
- Error concepts organized around Phenomenon (Failure / Deviation)
- Cause (Defect / Fault) attached to observed phenomena

Phase 2.9:
- Observation becomes the primary unit
- Phenomenon is no longer a first-class model element
- Facts are captured via:
  - Taxonomy (what is factually wrong)
  - Cause (how it failed)

This is a reallocation of responsibility, not a semantic change.

2. Treatment of Deviation

Article:
- Deviation treated as an explicit Phenomenon
- Quantitative difference from true or baseline value

Phase 2.9:
- Deviation is not a named core concept
- Represented implicitly via:
  - Taxonomy (e.g. FormatError / Invalid)
  - Observation properties and message

Deviation was absorbed into extensible factual recording,
avoiding premature specialization.

3. Defect / Fault Positioning

Article:
- Defect / Fault listed explicitly under Cause
- Strong linkage to ISO/IEC 24765 terminology

Phase 2.9:
- Observation.Cause is limited to failure mechanism
- Defect / Fault are treated as:
  - interpretative concepts
  - design / quality analysis vocabulary
- They are intentionally excluded from first-order Observation facts

This separation prevents responsibility, blame, or design judgment
from leaking into observations.

4. Mistake Handling

Both models agree:
- Mistake (human error) is not directly observed
- It is inferred indirectly through
  Mistake → Defect → Fault → Failure / Deviation chains

No change was introduced here.

Terminology Mapping (Stable)
----------------------------

Mistake   : Human cause (not observed)        → Same
Defect    : Cause                             → Interpretation-level
Fault     : Cause                             → Interpretation-level
Failure   : Phenomenon                        → Observation (Taxonomy-based)
Deviation : Phenomenon                        → Observation extension
Error     : Generic term                      → Same

Conclusion
----------

The Phase 2.9 redesign preserves the conceptual integrity of the previously
published Error Concept article while refining:

- semantic boundaries
- responsibility separation
- execution and DSL suitability

The relationship between the two is best described as:

Conceptual Model → Executable Observation Model

The article remains valid as a theoretical foundation, and Phase 2.9 represents
its practical, implementation-oriented evolution.

Open Design Questions (Deferred)
--------------------------------

1. Phenomenon abstraction (Observability)

Phenomenon was originally introduced to support observability concerns
by explicitly modeling externally visible phenomena such as Failure
and Deviation.

During the refinement of Conclusion-centered error routing,
Phenomenon gradually dissolved into Observation
(Taxonomy + factual fields) and was removed.

This was not a deliberate rejection of Phenomenon,
but a consequence of prioritizing error-routing precision.

Reintroduction or redesign of Phenomenon should be reconsidered
in a dedicated Observability design phase.

2. Descriptor / Aspect as debug-oriented metadata

Descriptor / Aspect were originally introduced
to support debugging and human inspection by attaching
structured, explanatory metadata to observations and conclusions.

They were dropped during the current refactoring
as a result of simplifying the core model and resolving compilation flow,
not because their utility was disproven.

Descriptor / Aspect (or an equivalent mechanism)
should be reconsidered explicitly from the perspective of
debuggability and inspection tooling.

