======================================================================
Goldenport DetailCode – Semantic Error Classification Reference
======================================================================

This document summarizes the design intent, structure, and usage of
Goldenport’s DetailCode system.

The purpose of this document is NOT to replicate Goldenport’s implementation,
but to preserve and reinterpret its semantic error-classification model
for use in SimpleModeling core, CNCF, and CLI.

----------------------------------------------------------------------  
1. What is DetailCode?
----------------------------------------------------------------------

DetailCode is a *semantic classification of failure*.

It answers questions such as:

- What kind of error is this?
- Where did it occur?
- Who should react to it?
- What kind of reaction is appropriate?

DetailCode complements StatusCode:

- StatusCode → external / protocol-facing (HTTP, API)
- DetailCode → internal / semantic-facing (meaning, responsibility)

----------------------------------------------------------------------  
2. Structural Overview
----------------------------------------------------------------------

Goldenport DetailCode is composed of five dimensions:

- Category
- Site
- Incident
- Application (optional)
- Reaction

Conceptually:

  DetailCode =
    (Category, Site, Incident, [Application], Reaction)

Each dimension represents a different semantic axis.

----------------------------------------------------------------------  
3. Category – Error Domain
----------------------------------------------------------------------

Category classifies the *domain of failure*.

Examples:

- Success
- ArgumentError
- ResultError
- StateError
- ServiceError
- SystemError

Key intent:

- Category distinguishes *who is responsible*
- It is coarse-grained and stable

In SimpleModeling core:
- Category is essential
- Should be preserved (possibly as a reduced enum)

----------------------------------------------------------------------  
4. Site – Where the Error Occurred
----------------------------------------------------------------------

Site represents *where* the error occurred.

Examples:

- argument
- operation
- service
- component
- database
- network
- config
- externalservice

Key intent:

- Site is diagnostic and operational
- Useful for logging, observability, routing

In SimpleModeling:
- Site is OPTIONAL in core
- Site belongs primarily to CNCF and runtime layers

----------------------------------------------------------------------  
5. Incident – What Happened
----------------------------------------------------------------------

Incident describes *what actually went wrong*.

Examples:

- syntaxerror
- invalid
- missing
- duplicate
- notfound
- unsupported
- ioerror
- defect
- precondition
- postcondition

Key intent:

- Incident captures the *nature* of the failure
- This is the most semantically expressive dimension

In SimpleModeling core:
- Incident SHOULD be preserved
- Incident is critical for AI reasoning and explanation

----------------------------------------------------------------------  
6. Application Code – Application-Specific Extension
----------------------------------------------------------------------

Application code is an optional, numeric + symbolic extension.

Purpose:

- Allow application-specific error namespaces
- Avoid collision between subsystems

Characteristics:

- Optional
- Rarely used
- Adds operational complexity

In SimpleModeling:
- Application code should NOT exist in core
- Reserved for CNCF or application-specific layers

----------------------------------------------------------------------  
7. Reaction – Expected Response
----------------------------------------------------------------------

Reaction defines *who should react and how*.

It is composed of:

- Stakeholder
- Action

Stakeholders:
- client
- application manager
- system administrator
- system developer

Actions:
- input
- retry
- config
- recover

Examples:

- client-input
- systemdeveloper-recover

Key intent:

- Reaction encodes *operational intent*
- It replaces ad-hoc error-handling rules

In SimpleModeling:
- Reaction is essential
- Reaction can replace ReactionStrategy
- Reaction should be part of Conclusion semantics

----------------------------------------------------------------------  
8. Relationship to Fault
----------------------------------------------------------------------

Fault represents a *concrete failure instance*.

- Faults carry:
  - message
  - parameters
  - implicit StatusCode
  - Reaction

- DetailCode represents the *classification*
- Fault represents the *occurrence*

Multiple Faults may map to:
- one DetailCode
- or multiple DetailCodes aggregated in Conclusion

----------------------------------------------------------------------  
9. Relationship to Conclusion
----------------------------------------------------------------------

Conclusion is the *semantic summary of execution*.

Goldenport pattern:

- Faults → aggregated
- DetailCode → derived / selected
- StatusCode → determined
- Reaction → chosen

In SimpleModeling:

- Conclusion SHOULD own DetailCode(s)
- Faults SHOULD NOT escape into public APIs
- Consequence carries Conclusion, not Faults

----------------------------------------------------------------------  
10. Core vs CNCF Responsibility Split
----------------------------------------------------------------------

Recommended split:

Core:
- Category
- Incident
- Reaction
- Minimal DetailCode (no Site, no Application)

CNCF:
- Site
- Application code
- Operational enrichment
- Mapping to observability / tracing

----------------------------------------------------------------------  
11. Design Lessons Extracted
----------------------------------------------------------------------

Key ideas worth preserving:

- Error classification is *orthogonal* to error message
- Reaction is a first-class semantic concept
- Error handling is declarative, not imperative
- Validation, CLI, config, and runtime share the same semantic model

Key ideas NOT to port directly:

- Full numeric code composition
- Scalaz-specific ordering
- Tight coupling with HTTP semantics

----------------------------------------------------------------------  
12. Mapping Guidance for SimpleModeling
----------------------------------------------------------------------

Goldenport → SimpleModeling (core):

- DetailCode.Category   → ErrorCategory
- DetailCode.Incident   → Incident
- DetailCode.Reaction   → Reaction
- Site / Application    → CNCF extension

Goldenport Fault → SimpleModeling Fault:
- Keep message + parameters
- Attach Category / Incident / Reaction

----------------------------------------------------------------------  
13. Status
----------------------------------------------------------------------

This document is a semantic reference.

- It guides design decisions
- It does not mandate exact structure
- It evolves as core / CNCF mature

Goldenport DetailCode is treated as a *conceptual asset*,
not a legacy dependency.

----------------------------------------------------------------------  
14. DetailCode and Observability
----------------------------------------------------------------------

DetailCode is especially valuable in modern cloud-native observability.

While AI-driven log analysis has reduced the need for humans to manually
inspect logs, DetailCode serves as a *semantic compression layer* that
improves both human and machine understanding.

Key points:

- Error codes alone can summarize both the phenomenon and the expected reaction
  - Category + Incident describe *what happened*
  - Reaction describes *what should be done*
- This enables:
  - Fast classification without parsing free-text messages
  - Deterministic routing for alerts and automation
  - Stable aggregation keys for metrics and SLOs

Design implications:

- Constraining errors into structured codes encourages observable design
- Developers are forced to think about responsibility and reaction at design time
- Logs, traces, metrics, and AI analysis can share a common semantic key

In cloud environments:

- Reaction = client-input
  → No alert, 4xx aggregation
- Reaction = retry
  → Automatic retry, rate monitoring
- Reaction = recover / escalate
  → Human intervention, paging

Although AI can infer meaning from raw logs, DetailCode reduces inference cost
and increases precision by providing explicit semantic intent.

Therefore, DetailCode should be viewed not as a legacy artifact, but as an
*observability-oriented semantic code system* suitable for cloud and AI-era
applications.

======================================================================
END OF DOCUMENT
======================================================================
