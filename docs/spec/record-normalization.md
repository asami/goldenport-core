# Record normalization and collection conversion

Status: Normative contract for Core Phase 3 / RCA-01-02A and RCA-03 entry points. The connected Step
passed representative validation (39 tests, three suites), affected accumulator
validation (123 tests, nine suites; two existing pending placeholders), and its
independent protected focused Step review on 2026-10-18. RCA-03 selected
validation passed 176 tests across 16 suites with four existing pending cases.
The independent Step review's HOCON integer-roundtrip blocker was corrected;
104 tests across four suites and the fresh focused closure review passed.
These counts overlap and are not additive. The sole independent Phase full
review passed. The final timestamp-projection regression was repaired at the
Observation output adapter; 36 tests across three suites and fresh focused
closure passed. Final repository-full `sbt --batch test` passed 575 tests across
156 suites, with zero failures and 125 existing pending scenarios. Core Phase 3
is closed by its local release commit; no publication or CNCF adoption is claimed.

The executable specification is
[RecordNormalizationSpec.scala](../../src/test/scala/org/goldenport/record/RecordNormalizationSpec.scala).
The entry-point and ordinary IO scenarios are in
[RecordEntryPointSpec.scala](../../src/test/scala/org/goldenport/record/RecordEntryPointSpec.scala).
The canonical datatype and declared-format rules are in
[structured-data-types.md](structured-data-types.md).

## Connected route

`Record.create`, `Record.createFull` and `Record.data` MUST omit native-null
input attributes and retain empty child Records and empty collections. Retained
fields MUST remain in input order. Empty String, String `"null"`, Unicode,
false and zero are present values.

`XSemiStructuredData.normalizer` MUST traverse construction-normalized Record
fields directly, validate supported values and preserve nested empty
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

## Shared construction boundary

Record MUST remain the field-vector case class with its existing Product,
equality and extractor behavior. Its primary constructor is private. Public
construction and immutable copy use these signatures:

```scala
Record(fields: Vector[Field] = Vector.empty): Record
Record.fromProduct(product: Product): Record
Record.fromFieldsC(fields: Vector[Field]): Consequence[Record]
Record.createC(xs: Seq[(String, Any)]): Consequence[Record]
record.copy(fields: Vector[Field] = record.fields): Record
```

`fromFieldsC` is the single construction normalization kernel. It MUST reject
an absent field vector, null Field, null key and absent or unsupported Field.Value
with `Consequence.valueInvalid(message)`. Only Field.Value.Single is supported.
A Single containing native null MUST be omitted at this input boundary. All
other scalar and opaque atoms MUST remain unchanged; construction MUST NOT
inspect application models or stringify them. Duplicate fields and their order
remain supported by Record; canonical XSemiStructuredData conversion rejects
duplicate keys separately.

Construction MUST recursively own Scala collection.Seq and native Array contents
as immutable Vectors, including mutable sequence inputs and primitive arrays.
Actual Cats NonEmptyVector values MUST retain their NonEmptyVector shape with
recursively owned elements. Existing nested Records are already normalized and
remain Records. Native-null elements at any sequence depth MUST fail rather than
be removed; independent failures MUST aggregate through Consequence.zipN. The
stored value MUST retain its original valid contents if the caller later mutates
the input array or mutable sequence. Empty collections, empty child Records,
false, zero and String `"null"` remain present.

Public apply, create, createFull, create(Map), data, dataOption, dataAuto and copy
MUST reach this kernel. Convenience APIs TAKE the result; C APIs MUST return the
original structured Consequence failure directly. The existing public product
constructor, called directly or through `Mirror.ProductOf[Record]`, MUST adapt
the Product's first element as the field Vector and call apply, reaching the
same kernel. Existing Product shape and cast behavior remain unchanged.
`createC` MUST reject an absent input sequence or native-null tuple with
`Consequence.valueInvalid(message)`; tuple admission MUST aggregate through
`Consequence.zipN` before the fields enter `fromFieldsC`.
`dataOption` omits None;
`dataAuto` unwraps its existing Some/None convention and omits None and Some(null).
`dataAuto` MUST retain empty structures, including the real ContentAttributes
projection's empty references collection.

appendField, upsertSingle, concatenation and filtering MUST preserve construction
invariants. An append of null leaves existing fields unchanged; replacement of a
key by null removes every existing occurrence of that key. Concatenation retains
duplicates and order. Internal removal uses removeKeys, never a null instruction.
Ordinary getters, typed readers, maps, string conversion and path access MUST
consume the owned invariant without routine native-null filtering. Missing-value
behavior and literal path-key precedence remain unchanged. Field.create and
Record.update/complement placeholders remain outside this change.

## Ordinary ingress and output

RecordDecoder single JSON/YAML APIs share the explicit canonical converters.
Plural APIs require arrays/lists of objects; auto APIs additionally accept one
object. Explicit empty arrays/lists succeed. Scalar/null roots, null list
elements and empty YAML documents MUST fail; yamlRecords MUST NOT interpret an
empty document or null root as an empty collection. String, Bag and InputStream
wrappers retain their APIs. Exact JSON/YAML numbers become unlimited-precision
BigDecimals. YAML validates all keys and duplicates before omitting null-valued
attributes, retains alias-cycle protection and rejects unsupported tags and
non-string or merge keys.

Strict literal HOCON retains its no-includes, no-substitutions and no-environment
contract and exact integer-token adaptation. Ordinary RecordSourceLoader and
RecordImportDecoder retain their existing parseString(...).resolve() features,
then share direct conversion of the whole Config root. That converter performs
no IO, resolution or numeric-looking-string guessing. Both routes preserve empty
objects, quoted dotted keys and nested list objects, omit null object members
before recursion and reject null list positions. HOCON import selects the first
nonempty records/items/data container, otherwise the whole root; scalars become
Records with a value field. Schema coercion remains a later existing step, so an
age coerced to XInt remains Int20. TOML converts foreign maps/collections at
ingress with Consequence aggregation, omitting null map members and rejecting
null sequence elements while retaining existing scalar/time and table behavior.

Canonical row JSON and TSL exports render JSON arrays from aggregated
XSemiStructuredData.toJson results. YAML row export uses per-record canonical
YAML with deterministic list indentation, `[]` for no rows and an empty mapping
item for Record.empty. HOCON uses canonical JSON objects inside its existing
records list and MUST recursively emit integral numbers outside Long range as
exact unquoted decimal numeric tokens with scale 1, preserving numeric type
through ordinary Config loading. This emission MUST NOT infer numbers from
Strings or change full-config resolution, strict literal parsing or public APIs.
Unknown, raw-Json, duplicate and nonfinite canonical values MUST
return their structured failures without stringification. Nested Record,
NonEmptyVector, Seq and Array text cells use the shared semantic value-to-JSON
converter; missing CSV/Excel cells remain Option until rendered as empty String.
Content types, extensions, export schemas and ordinary scalar cell display stay
unchanged.

RecordEncoder XML traverses NonEmptyVector before ordinary Iterable and retains
the existing record/field/item shape, name escaping and scalar lexical display.
It does not require all XML display atoms to satisfy XSemiStructuredData. Native
null required by foreign output APIs, such as Elem's prefix or YAML output
adapters, is confined to those adapters.

## Constructor compatibility and CNCF handoff

Constructor privacy is a source/ABI-affecting migration. Downstream callers MUST
use Record(...), Record.fromFieldsC or public copy instead of `new Record` or
anonymous subclasses. No downstream source or ABI transparency is claimed.

Concrete CNCF115 handoff input:
`DurableJobOperationResponseCodecSpec.scala:115` constructs
`new Record(Vector.empty)` with a hostile anonymous subclass; lines 92-145 also
intentionally construct malformed null fields. CNCF must redesign these fixtures
to test its closed-codec contract without invalid Record instances or subclass
construction. Core MUST NOT weaken the construction invariant to preserve those
fixtures. This Core Phase edits no CNCF source and establishes neither downstream
validation/adoption nor dependency publication.
