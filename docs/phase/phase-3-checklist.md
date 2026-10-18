# Phase 3 Checklist - Record Collections and Attribute Normalization

Status: IN_PROGRESS
Owner: goldenport-core
Plan: [Phase 3](phase-3.md)
Update rule: Check only outcomes supported by actual implementation, validation and the applicable independent acceptance; retain remaining Phase gates as open.

RCA-01 and RCA-02 are accepted as the connected Step RCA-01-02 on 2026-10-18:
39 representative tests across three suites and 123 accumulator tests across
nine suites passed, and the independent protected focused Step review passed.
Two pre-existing pending placeholders remain unchanged. RCA-03 subsequently
passed selected validation (176 tests, 16 suites; four existing pending cases).
Its HOCON integer-roundtrip correction passed 104 tests across four suites and
the independent focused closure review; the counts overlap and are not additive.
The [non-normative CNCF115 handoff](../spec/record-normalization-cncf115-handoff.md)
and integrated focused evidence passed independent documentation acceptance for
the originally declared final Step. This is not Phase closure: RCA-04/05 stay open
until the mandatory Phase gates complete. Core owns the sole comprehensive
Phase full review, repository-full validation and final release closure; these
are Phase gates, not an extra successor Step or a dependency on CNCF115 completion.

- [x] **RCA-01 - Contract migration specified.** Working specifications replace
  null-attribute retention with omission, preserve empty objects/arrays, define
  null-element failure and distinguish Record from XJson. Acceptance examples
  cover the complete route and typed/non-typed collection conversion. Native
  null is forbidden in normal logic; input/output adapters own its handling.
- [x] **RCA-02 - Connected route works.** Nested Record with NonEmptyVector
  encodes as an array and decodes with correct values and order; null attributes
  disappear recursively while empty children remain. Explicit nonempty reads
  succeed or fail by cardinality through Consequence.
- [x] **RCA-03 - Entry points agree.** Actual constructors, mutation-style
  immutable APIs, codecs and canonical normalizers have one documented policy;
  direct Field/createFull paths do not accidentally reintroduce null attributes.
  Affected callers/specifications are migrated. JSON/YAML/HOCON and existing
  sequence-capable XML output are covered proportionately. Internal callers
  use typed absence/removal instead of null sentinels; boundary conversion
  supplies the normal logic without repeated internal null checking.
- [ ] **RCA-04 - Integrated behavior accepted.** Focused executable scenarios
  and property-based normalized roundtrip/idempotence/order laws pass. Null
  array positions are never silently removed. XJson, exact numbers and unrelated
  scalar behavior remain intact. Independent review and required release
  validation pass, with actual results recorded.
- [ ] **RCA-05 - Handoff and closure recorded.** CNCF Phase 115 has the selected
  Core dependency and API/migration guidance; prior evidence remains historical.
  Required release/closure records identify delivered behavior and any separately
  owned remaining work. No consumer completion is inferred.
