# Phase 3 Checklist - Record Collections and Attribute Normalization

Status: PLANNED
Owner: goldenport-core
Plan: [Phase 3](phase-3.md)

All items are OPEN. Creating this plan does not accept current implementation.

- [ ] **RCA-01 - Contract migration specified.** Working specifications replace
  null-attribute retention with omission, preserve empty objects/arrays, define
  null-element failure and distinguish Record from XJson. Acceptance examples
  cover the complete route and typed/non-typed collection conversion.
- [ ] **RCA-02 - Connected route works.** Nested Record with NonEmptyVector
  encodes as an array and decodes with correct values and order; null attributes
  disappear recursively while empty children remain. Explicit nonempty reads
  succeed or fail by cardinality through Consequence.
- [ ] **RCA-03 - Entry points agree.** Actual constructors, mutation-style
  immutable APIs, codecs and canonical normalizers have one documented policy;
  direct Field/createFull paths do not accidentally reintroduce null attributes.
  Affected callers/specifications are migrated. JSON/YAML/HOCON and existing
  sequence-capable XML output are covered proportionately.
- [ ] **RCA-04 - Integrated behavior accepted.** Focused executable scenarios
  and property-based normalized roundtrip/idempotence/order laws pass. Null
  array positions are never silently removed. XJson, exact numbers and unrelated
  scalar behavior remain intact. Independent review and required release
  validation pass, with actual results recorded.
- [ ] **RCA-05 - Handoff and closure recorded.** CNCF Phase 115 has the selected
  Core dependency and API/migration guidance; prior evidence remains historical.
  Required release/closure records identify delivered behavior and any separately
  owned remaining work. No consumer completion is inferred.
