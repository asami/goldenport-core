# External value conversion

Status: PHASE-115 / RCP-CONSUMERS / LIB-CORE API and executable specifications
focused-validated on 2026-10-18: selected Core conversion repair passed 79/79 in
six suites. The connected consumer Step is focused-validated and passed independent
complete protected focused RCP-CONSUMERS Step review. Local commits, all four
repository full tests, the sole comprehensive Phase 115 review and final release
remain pending; all five RCP obligations remain OPEN and Phase 115 IN_PROGRESS. This status preserves
closed Core Phase 3 acceptance and does not establish final Phase 115 acceptance.

The public projection lives in `org.goldenport.convert.ExternalValueEncoder`:

```scala
type Adapter = PartialFunction[Any, Consequence[Any]]
def encodeC(value: Any): Consequence[Any]
def encodeC(value: Any, adapter: Adapter): Consequence[Any]
```

The overload without an adapter uses `PartialFunction.empty`. The encoder MUST
project values for external Record construction. It MUST NOT act as a JSON/YAML
codec or replace canonical `XSemiStructuredData` validation. Existing typed Json
of every shape, including Json.Null and embedded nulls, and construction-normalized
Records MUST remain values without codec roundtrips or revalidation. The caller's
declared semantic datatype owns canonical validation of a Record. In particular,
retaining raw Json here does not make raw Json members valid canonical Record data.
`RecordEncoder` and `RecordExportEncoder` retain their existing contracts.

## Present values, absence and errors

String, Boolean, Byte, Short, Int, Long, finite Float/Double, Scala BigInt and
BigDecimal and JDK BigInteger/BigDecimal MUST retain their values and numeric
precision without conversion to Double. Char MUST use its explicit character
text. Nonfinite Float/Double and a native-null root MUST fail through named
structured Consequence utilities.

None MUST remain None. Some MUST recursively project its child while retaining
Some. Empty collections MUST succeed as present empties. Seq, native Array and
actual Cats NonEmptyVector MUST recursively project into Vector while retaining
order and duplicates. Set MUST project to Vector without a sorted-order promise.
A native-null sequence element or an element projected to absent None MUST fail
the whole conversion without filtering, position changes or a successful empty
default. Json.Null is a present sequence value. Independent child failures MUST
aggregate with `Consequence.zipN`.

Maps MUST remain keyed Map associations with String keys and recursively projected
values. Other key types MUST fail without invoking key.toString. Map MUST NOT
reconstruct ordered or duplicate-bearing Record fields. Record construction
continues to own structural normalization, independently of this projection.

## Core protocols and model-owned extensions

I18nLabel, I18nTitle, I18nBrief, I18nSummary, I18nDescription and I18nText MUST use
`toI18nString.toRecord`. MimeType uses print; Charset uses name; Locale uses
toLanguageTag; TimeZone uses getID; ContentBody and ContentMarkup use value.
ObjectId MUST recursively project its id. NameAttributes MUST project name,
label, title, code, alias and slug through this same encoder and Record.dataAuto.
DescriptiveAttributes MUST likewise project headline, brief, summary, description,
lead, abstract, remarks and tooltip. ContentReferenceOccurrence and ContentAttributes
MUST use their RecordPresentable contract, including present empty references.

For a value outside these canonical Core branches, the explicit adapter MUST be
consulted once before generic RecordPresentable or Presentable fallback. It is a
typed ownership hook for a model library, allowing nominal values to project
underlying data and allowing that library wrapper to delegate nested values
recursively. It MUST NOT infer model meaning from payload shapes. Core MUST NOT
acquire model, CNCF, SQL, provider, Update or Condition dependencies, or a global
registry, cache or control state.

RecordPresentable MUST invoke toRecord under Consequence and accept only a
non-null Record result. Presentable MUST use its explicit print contract under
Consequence. Foreign callbacks and adapters MUST have their exceptions contained
by existing Consequence admission. Unknown objects MUST fail with a fixed,
non-leaking message without arbitrary toString, Product inspection, reflection
or serialization. Explicit protocol projection does not authorize arbitrary
unknown-object text conversion.

## Reader boundary and executable evidence

The ordered read contract, including missing/empty/error distinctions, explicit
scalar promotion and identical ExecutionContext forwarding, is specified in
[record-normalization.md](record-normalization.md#ordered-reader-and-application-adapter-policy).
Application Record decoders retain application ownership and do not replace the
canonical Record reader. The model-owned external adapter is similarly explicit.

[ExternalValueEncoderSpec.scala](../../src/test/scala/org/goldenport/convert/ExternalValueEncoderSpec.scala)
specifies the public projection, independent expected values, failure containment
and a minimum of 100 successful order/duplicate/raw-Json/exact-value property
samples with zero discards.
[CollectionValueReaderSpec.scala](../../src/test/scala/org/goldenport/convert/CollectionValueReaderSpec.scala)
specifies the connected reader behavior. The selected 79/79 Core run covers
these conversion/reader scenarios. Model 54/54, Modeler 33/33 and actual generated
consumer validation are separate, overlapping focused scopes; none substitutes
for independent connected Step acceptance or final four-repository validation.
