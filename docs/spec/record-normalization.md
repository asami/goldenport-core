# Record normalization and collection conversion

Status: Normative contract for Core Phase 3 / RCA-01-02A. The connected Step
passed representative validation (39 tests, three suites), affected accumulator
validation (123 tests, nine suites; two existing pending placeholders), and its
independent protected focused Step review on 2026-10-18. RCA-03, Phase-full
validation/review and release acceptance remain pending.
This document does not establish completion of RCA-03 or the Phase.

The executable specification is
[RecordNormalizationSpec.scala](../../src/test/scala/org/goldenport/record/RecordNormalizationSpec.scala).
The canonical datatype and declared-format rules are in
[structured-data-types.md](structured-data-types.md).

## Connected route

`Record.create`, `Record.createFull` and `Record.data` MUST omit native-null
input attributes and retain empty child Records and empty collections. Retained
fields MUST remain in input order. Empty String, String `"null"`, Unicode,
false and zero are present values.

`XSemiStructuredData.normalizer` MUST traverse Record fields directly, omit
null attributes recursively, validate supported values and preserve nested empty
objects and arrays. A child emptied by omission MUST remain a present empty
Record. Supported exact numeric inputs MUST normalize to unlimited-precision
BigDecimal. Seq, native Array and Cats NonEmptyVector inputs MUST normalize to
ordered Vectors. Duplicate keys, unknown members, raw Json-valued Record members
and nonfinite numbers MUST fail with structured Consequence outcomes; independent
field and element failures MUST aggregate through `Consequence.zipN`.

RecordEncoder JSON and YAML Consequence APIs MUST consume the shared canonical
`XSemiStructuredData.toJson` conversion. Public String convenience wrappers and
YAML rendering retain their existing APIs. Supported nested NonEmptyVectors MUST
encode as ordinary arrays, recursively and in order, without stringification.

Canonical JSON decoding accepts object roots only, omits null object attributes
before recursively decoding children, and returns Vector for every untyped array.
Explicit JSON/YAML/HOCON text decoding MUST apply the same canonical contract.
A null sequence element at any depth MUST fail; it MUST NOT be removed or shift
the following elements. A top-level null remains invalid for Record.

An explicit `ValueReader[NonEmptyVector[A]]` MUST accept NonEmptyVector, Seq and
native Array inputs only. It MUST require nonempty input, preserve order, and
decode every element using the supplied ValueReader[A]. Empty or wrong-shaped
input MUST return `Consequence.valueInvalid(message)`. Native-null elements MUST
fail before an element reader can accept them, including ValueReader[Any].
Independent element failures MUST aggregate. Contextual reads MUST forward the
same supplied ExecutionContext to each element's contextual reader.

The route through real `Record.asC`, `getAsC` and `getAsContextC` MUST expose these
semantics. A present empty array requested as NonEmptyVector is a failure; a
missing optional field remains successful None. `Record.getVector` MUST also
recognize actual Cats NonEmptyVector values without requiring caller conversion.

## Laws and separate JSON semantics

Normalization MUST be idempotent. Encoding followed by canonical decoding MUST
equal the normalized Record. Null-bearing JSON object roundtrips MUST equal an
independent recursive omission of null object attributes. Retained object-key
order and every array element's order MUST survive the connected JSON route.
Property specifications MUST distinguish nullable object attributes from
non-null array elements and compute expected semantics independently of production
normalizers. HOCON lexical field order is not promised.

XJson and ValueReader[Json] MUST retain existing Json instances for every shape,
including Json.Null and null-containing arrays and objects. ValueReader[String]
MUST retain lexical text exactly. Those contracts do not authorize raw Json
members or null sequence positions inside a Record-owned value.

## Explicit later RCA-03 work

This Slice retains the public Record case class, constructor and fields shape,
copy, dataAuto, merge/concatenation behavior and placeholder methods. Their
remaining entry-point harmonization, affected caller migration, ordinary decoder
and source-loading consistency and sequence-capable XML export belong to RCA-03.
Native-null boundary handling does not authorize null as an internal removal
instruction or a patch-policy redesign. No constructor/ABI redesign, downstream
CNCF adoption or complete Phase acceptance is inferred from this connected route.
