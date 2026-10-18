# Phase 3 Checklist - Record Collections and Attribute Normalization

Status: CLOSED
Owner: goldenport-core
Plan: [Phase 3](phase-3.md)
Update rule: Retain the actual Core implementation, review and validation closure basis. Publication and CNCF adoption require their own separately owned work.

RCA-01 and RCA-02 are accepted as the connected Step RCA-01-02 on 2026-10-18:
39 representative tests across three suites and 123 accumulator tests across
nine suites passed, and the independent protected focused Step review passed.
Two pre-existing pending placeholders remain unchanged. RCA-03 subsequently
passed selected validation (176 tests, 16 suites; four existing pending cases).
Its HOCON integer-roundtrip correction passed 104 tests across four suites and
the independent focused closure review; the counts overlap and are not additive.
The [non-normative CNCF115 handoff](../spec/record-normalization-cncf115-handoff.md)
and integrated focused evidence passed independent documentation acceptance for
the originally declared final Step. The sole comprehensive Phase review passed.
The first full test exposed a timestamp projection regression; the existing
Observation output adapter now emits its known Instant as the existing ISO
string. Focused regression validation passed 36 tests across three suites and
fresh independent focused closure passed. Final `sbt --batch test` passed 575
tests across 156 suites, with zero failures or aborted suites and 125 existing
pending scenarios; both exits were zero and the serial lock was released.
These actual results and the distinct local Phase release commit form the
closure basis. The earlier failed run is retained as history. No extra Step,
publication or dependency on CNCF115 completion is introduced.

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
- [x] **RCA-04 - Integrated behavior accepted.** Focused executable scenarios
  and property-based normalized roundtrip/idempotence/order laws pass. Null
  array positions are never silently removed. XJson, exact numbers and unrelated
  scalar behavior remain intact. Independent review and required release
  validation pass, with actual results recorded.
- [x] **RCA-05 - Handoff and closure recorded.** CNCF Phase 115 has the selected
  Core dependency and API/migration guidance; prior evidence remains historical.
  Required release/closure records identify delivered behavior and any separately
  owned remaining work. No consumer completion is inferred.
