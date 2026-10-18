# Record normalization: CNCF Phase 115 consumer handoff

Status: NON-NORMATIVE migration guide for the accepted Core Phase 3 source
outcome. This guide summarizes existing contracts; it adds no behavior or
upstream feature requirement. RCA-04/05 documentation passed independent Step
acceptance on 2026-10-18. The sole comprehensive Phase review, final projection
repair's independent focused closure and repository-full validation passed;
Core Phase 3 is closed by its local release commit.

The authorities are [Record normalization](record-normalization.md) and
[canonical structured datatypes](structured-data-types.md). Delivery state is
tracked in [Core Phase 3](../phase/phase-3.md) and its
[checklist](../phase/phase-3-checklist.md). Consumer work belongs to
[CNCF Phase 115](../../../cloud-native-component-framework/docs/phase/phase-115.md),
whose recorded status is PLANNED. Separate nonblocking maintenance remains in
the [original Core hygiene journal](../journal/2026/10/2026-10-18-phase-3-hygiene-follow-up.md).

## Select the source outcome before the artifact

The dependency coordinate remains unchanged in [build.sbt](../../build.sbt):

```scala
"org.goldenport" %% "goldenport-core" % "0.4.3-SNAPSHOT"
```

Select the final Core Phase 3 closure commit on `main` containing this guide,
including the Observation timestamp projection correction and its regression.
The accepted RCA-03 source ancestor is
`dc14bf9554b45c881e674b35633fbf2b0be169d4`, including the HOCON row correction;
that ancestor alone does not contain the final timestamp correction.
The earlier connected RCA-01/02 Step is
`2150f9377c4c51eb7ce7114df30a350163492512`; the original Phase base is
`8102fe40e2b052c637f89c929fec9a7434f24b57`.

This Phase has performed no artifact publication, publishLocal or dependency
refresh. A local or remote artifact at this same SNAPSHOT coordinate may be
older. Consumers need to deliberately select this source outcome and separately
refresh or publish the intended dependency before trusting the coordinate.
There is no claim of a newly available binary, release version bump or downstream
build/adoption. Local Core Phase closure is distinct from library publication.

## Public construction and accepted behavior

| Consumer surface | Migration guidance |
| --- | --- |
| `new Record(...)` or an anonymous Record subclass | The primary constructor is private. Use public `Record(...)`, `Record.fromFieldsC`, `Record.createC` or `record.copy`. |
| Field-vector and Product construction | Fields, case-class equality and extractor behavior remain; existing `fromProduct` and `Mirror.ProductOf[Record]` construction also reach normalization. Source and ABI compatibility are not transparent. |
| Recoverable construction failure | `fromFieldsC` and `createC` preserve structured invalid `Consequence` failures. Convenience APIs TAKE the result and throw the same structured failure on invalid input. |
| Hostile consumer fixtures | Exercise the actual foreign/codec input boundary; do not forge malformed Record instances or bypass Core construction. |

Native-null construction attributes and JSON-null object attributes are omitted
at their actual boundaries, recursively. Empty Records, empty lists, empty
Strings, String `"null"`, false and zero remain present; retained field and
element order survives. Null sequence positions fail, without filtering or
index shifts. Construction recursively owns Seq and native Array inputs as
immutable Vectors; an actual NonEmptyVector retains its shape with owned
elements. Canonical semantic normalization and untyped decoding use Vectors;
explicit NonEmptyVector typed reading checks nonempty cardinality.

Canonical conversion preserves exact numeric meaning, rejects unsupported
members, duplicate keys and nonfinite numbers, and aggregates independent
failures through `Consequence.zipN`. Construction and canonical validation have
their existing distinct responsibilities; consumers need no new normalizer.
Raw `XJson`, including `Json.Null` and null array positions, retains its separate
JSON preservation contract. A raw Json member is not a supported semantic
Record member.

Ordinary full-Config loading retains its existing parsing/resolution features
and preserves values declared as String. Strict literal HOCON decoding remains
a separate no-include/no-resolution route. The canonical HOCON row emitter
recursively renders signed integral numbers outside Long range as exact unquoted
numeric tokens with scale 1; it does not infer numbers from numeric-looking
Strings. JSON/YAML/HOCON exact-number and structured-failure contracts remain
those of the linked specifications.

## Provider absence and update instructions

Native JDBC/provider null belongs at the actual foreign ingress: convert it to
absence, an existing typed value or a `Consequence` error according to that
boundary's contract. Never transport native null into semantic Record logic.
Only the final output adapter materializes SQL NULL where the external API
requires it. Empty objects and arrays remain present values, not SQL NULL.

| Input meaning | Consumer responsibility |
| --- | --- |
| Missing patch field | Leave the existing value unchanged. |
| Existing explicit `Update.SetNull` | Preserve the typed clearing instruction; the final output adapter emits SQL NULL. |
| Existing CNCF bare-null patch syntax | Decode it into the existing explicit clearing instruction before semantic Record normalization can omit the attribute. |
| Null-valued semantic object attribute | Normalize to absence at the actual object input boundary; this is not a patch instruction. |
| Raw JSON `Json.Null` | Preserve it under the declared XJson contract; interpret it explicitly before using it as a business value. |

Core implements neither SQL policy nor patch policy. This handoff calls for no
database rewrite, compatibility mode or repeated internal null checks.

## Actual consumer fixture migration input

A read-only consumer inspection recorded on 2026-10-18
observed an anonymous `new Record(Vector.empty)` at line 115 of
[DurableJobOperationResponseCodecSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/job/DurableJobOperationResponseCodecSpec.scala).
Its lines 92–145 also contain deliberately malformed/null-field examples.
These source/ABI-sensitive fixtures need redesign at the actual closed-codec
input boundary, without invalid Record or subclass construction and without a
Core bypass.

The same inventory records these additional search hits for downstream
inspection:

- [EntityPersistentStoreMappingSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/entity/EntityPersistentStoreMappingSpec.scala): malformed Record/Field and `Single(null)` fixtures.
- [EntityMutationCaptureOutcomeSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/entity/EntityMutationCaptureOutcomeSpec.scala): a `Record(null)` fixture.
- [EntityStoreRowMappingSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/entity/EntityStoreRowMappingSpec.scala): malformed vector/Field/value fixtures.
- [EntityVersionedMutationOutcomeSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/entity/EntityVersionedMutationOutcomeSpec.scala): malformed Record and explicit-null identifier fixtures.
- [ContentBodyStoragePolicySpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/entity/ContentBodyStoragePolicySpec.scala): physical SQL NULL and business/audit null payload fixtures needing provider-versus-semantic inspection.
- [EntityCreateProviderContractSpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/datastore/EntityCreateProviderContractSpec.scala): an absent Field.Value fixture.

These are read-only search hits, not a completed downstream diagnosis or an
instruction to delete every null. Existing typed `Update.SetNull` cases, including
those inventoried in
[SqlMutationAcknowledgementSafetySpec.scala](../../../cloud-native-component-framework/src/test/scala/org/goldenport/cncf/datastore/SqlMutationAcknowledgementSafetySpec.scala),
remain explicit clearing instructions. No new search, consumer change or
adoption is claimed here.

## Integrated evidence and limits

- Native RCA-01/02 outcome `2150f937…`: 39 tests / 3 representative suites and
  123 tests / 9 accumulator suites passed, with two existing pending placeholders
  and protected focused independent Step acceptance.
- Native RCA-03 outcome `dc14bf9…`: selected coverage passed 176 tests / 16
  suites, with four existing pending cases. The
  original independent Step review
  returned FINDINGS for HOCON integer row roundtrips. The correction passed
  104 tests / 4 suites, including generated signed overflow and nested/quoted
  80/400-digit controls, followed by a
  fresh focused PASS
  and factual M0 status recording.
- These counts overlap; they are not an additive total. Earlier Workbench
  94 tests / 6 suites concern the historical null-preserving prerequisite,
  not proof of the new semantics.
- The sole comprehensive independent Phase review passed. The first full run
  passed 573 of 574 tests and exposed the Observation timestamp projection gap.
  The existing output adapter now renders its known Instant as its previous ISO
  string, leaving the typed model and strict canonical domain unchanged.
  Focused regression validation passed 36 tests / 3 suites and fresh independent
  focused closure passed. The final `sbt --batch test` passed 575 tests / 156
  suites, with zero failures or aborted suites and 125 existing pending cases;
  both exits were zero and the shared serial lock was released.
- HYG-CORE-P3-001 through HYG-CORE-P3-003 remain nonblocking and unresolved.
  The local Core Phase release records closure without artifact publication,
  a dependency refresh or consumer adoption.

## Downstream handoff checklist

The following actions belong to CNCF115; this guide executes none of them:

- [ ] Select the accepted Core source outcome and separately refresh/publish
  the intended dependency; verify which artifact is actually consumed.
- [ ] Migrate actual provider/JDBC and codec input boundaries, and redesign
  the affected hostile fixtures using valid public construction.
- [ ] Preserve missing-patch versus typed null-clearing behavior, including
  existing bare-null patch decoding before semantic normalization.
- [ ] Preserve raw JSON, NonEmptyVector cardinality, exact numbers, retained
  order and present empty structures under their declared contracts.
- [ ] Run CNCF115's own connected save/read/update route with its independently
  required executable specifications and review.

This document closes neither CNCF115 nor CNCF100. Core can close independently
after its own usable handoff and full Phase gates; downstream completion is not
an additional Core successor Step or closure dependency.
