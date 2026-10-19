# ESA62 structured operation carrier validation — 2026-10-19

The owning user admitted the bounded OperationDefinition correction under
`esa62-structured-operation-admission-fix-20261019@1`, supporting
`CPB-ESA62-COMMAND-JSON-CARDINALITY-001` and
`CPB-ESA62-COMMAND-RECORD-LEAF-001` in the original Phase 6.2 connected Step.

The actual protocol decoder supplies repeated structured values in one Property
as a Vector. Existing extraction previously passed that Vector as one document
to canonical scalar normalization, so generated request validation failed before
the selected collection reader could consume the request. OperationDefinition
now unwraps exactly one Vector only for canonical Json or SemiStructuredData
parameters that already allow multiple values. Its existing absence, required,
normalization and constraint branches retain semantic authority.

The executable specification exposes actual ResolvedValue outcomes using ordinary
OperationRequest construction and Property carriers. It authors omitted/empty,
singleton and ordered multiple values, Json.Null versus missing, scalar Json
arrays, exact nested Records, repeated-property equivalence and invalid carrier
or element rejection. Expected documents are literal fixtures, independent of
the production normalizer. Existing integer, enum and range scenarios remain.

Status: authored; validation pending. The parent owns focused library validation,
unchanged SNAPSHOT visibility refresh, the downstream normal generated consumer,
independent focused review and workflow transitions. This contribution does not
reopen a Core Phase or establish test success or Phase closure.

## Contribution acceptance — 2026-10-19

The preceding authored/pending status is historical. Actual library validation
041 passed 65 tests/5 suites. Independent focused result
`esa62-structured-operation-focused-result-20261019@1` is PASS and resolves both
structured-operation blockers: canonical structured Vector carrier validation,
ordered arrays, absence versus present Json.Null and named Record leaf semantics
are accepted with the actual downstream generated constructor evidence 052.
Earlier overlapping runs are not additive. This is the goldenport-core library
contribution to the connected Step; it does not reopen any Flutter Core Phase.

App Phase 6.2 remains OPEN/`in_progress`; Step2/3 local commits, sole
comprehensive Phase review, owning final full validation and distinct release
remain pending. Original native 001 uninstall/data loss and native 006/host 001
failed cleanup remain history. Only 52 October 9 capture files have proven
capture recovery; later audio/configuration/credentials and complete data
recovery remain unproven. Existing naming and publication/domain debt remains
separate and nonblocking.
