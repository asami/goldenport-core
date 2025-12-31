I18nContext
===========

status=stable
scope=core
audience=core / CNCF / infra / AI integration

----------------------------------------------------------------------
1. Purpose
----------------------------------------------------------------------

I18nContext defines how textual, numeric, and temporal information
is interpreted in terms of language-related semantics.

This context targets knowledge processing, search, comparison,
and identity determination, rather than UI rendering or translation.

----------------------------------------------------------------------
2. Design Principles
----------------------------------------------------------------------

I18nContext MUST be:

- Explicit (no implicit JVM or OS defaults)
- Immutable and value-backed
- Semantics-oriented (no I/O or implementation details)
- Independent from execution environment and runtime state
- Capable of treating Japanese/CJK text as first-class semantics

----------------------------------------------------------------------
3. Core Structure
----------------------------------------------------------------------

I18nContext groups language-related policies as follows:

- TextNormalizationPolicy
- TextComparisonPolicy
- DateTimeFormatPolicy

Locale MAY be included as auxiliary information, but MUST NOT be
the primary driver of text semantics.

----------------------------------------------------------------------
4. Responsibilities
----------------------------------------------------------------------

4.1 Text Normalization

Defines how textual input is transformed into a stable canonical form
for knowledge processing and search.

Delegated to TextNormalizationPolicy.

4.2 Text Comparison

Defines how normalized texts are compared, ordered, and evaluated
for equality.

Delegated to TextComparisonPolicy.

4.3 Date and Time Interpretation

Defines how date/time values are rendered as strings in a consistent,
semantics-aware manner.

Delegated to DateTimeFormatPolicy.

5. Explicit Non-Goals
----------------------------------------------------------------------

I18nContext does NOT include:

- Translation or language conversion
- ResourceBundle or message lookup
- UI formatting or presentation concerns
- Input validation rules
- JVM or OS implicit settings

----------------------------------------------------------------------
6. Typical Configurations
----------------------------------------------------------------------

6.1 Knowledge Processing (Japanese)

- TextNormalizationPolicy: JapaneseCanonical
- TextComparisonPolicy: JapaneseCollation
- DateTimeFormatPolicy: Iso8601

6.2 Security / Protocol Processing

- TextNormalizationPolicy: StrictBinary
- TextComparisonPolicy: BinaryCompare
- DateTimeFormatPolicy: Iso8601

----------------------------------------------------------------------
7. Relationship with ExecutionContext
----------------------------------------------------------------------

I18nContext is a component of ExecutionContext.

Core logic MUST access I18nContext via ExecutionContext and MUST NOT
directly rely on JVM defaults.

----------------------------------------------------------------------
8. Implementation Responsibility
----------------------------------------------------------------------

- core:
  - Defines the structure and semantics of I18nContext
  - Declares the policy types

- CNCF / infrastructure:
  - Provides concrete implementations using JVM, ICU, or external services

Core MUST NOT implement language processing algorithms.

----------------------------------------------------------------------
9. Rationale
----------------------------------------------------------------------

By explicitly modeling language semantics as policies,
I18nContext eliminates ambiguity caused by Locale-centric designs
and enables robust knowledge processing, search, and AI integration,
especially for Japanese and CJK languages.

Numeric formatting is intentionally not modeled as a first-class policy
at this stage. Locale-dependent numeric representation relies on
platform-standard mechanisms (e.g. Java NumberFormat / DecimalFormat)
and is treated as an implementation concern rather than a core
language-semantics policy.

END
----------------------------------------------------------------------
