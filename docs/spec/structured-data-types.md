# Canonical structured datatypes

Status: Normative Core canonical/external-format/typed-reader contract, revised for
Core Phase 3 / RCA-01-02A and RCA-03 entry points. The earlier Workbench PHASE-1 / STEP-2 / PREREQ-CORE-A,
PREREQ-CORE-B and PREREQ-CORE-C accumulator recorded 94/94 focused tests across six
suites and a protected focused independent conformance review (PASS). That historical
evidence applies to the earlier null-preserving prerequisite, not these Phase 3 semantics.
The Phase 3 connected Step passed 39 representative tests across three suites,
123 accumulator tests across nine suites (two existing pending placeholders),
and independent protected focused Step review on 2026-10-18. These are Step-level
results; the completed Core Phase outcome is recorded below.

## Stage Status

- Current status: The connected Record construction, JSON/YAML encoding, explicit
  decoding and nonempty typed-read route passed its Step validation and independent
  review. RCA-03 selected validation passed 176 tests across 16 suites with four
  existing pending cases. Its independent Step review's HOCON integer-roundtrip
  blocker was corrected, then 104 tests across four suites and the fresh focused
  closure review passed. These counts overlap and are not additive. The sole
  comprehensive independent Phase review passed. The final Observation timestamp
  output-projection regression passed 36 tests across three suites and fresh
  focused closure. Final repository-full `sbt --batch test` passed 575 tests
  across 156 suites, with zero failures and 125 existing pending scenarios.
  Core Phase 3 is closed by its local release commit; no downstream acceptance
  or publication is supplied.
- Owner: goldenport-core; the Workbench PHASE-1 / STEP-2 prerequisite remains historical context.
- Update rule: Advance this status from actual focused validation and independent
  Core review results. Generator and DataStore adoption retain their separate
  closure criteria; Core closure does not complete those consumers.

The canonical declarations are in [DataType.scala](../../src/main/scala/org/goldenport/schema/DataType.scala).
Their package-private JSON conversion is in [StructuredDataJson.scala](../../src/main/scala/org/goldenport/schema/StructuredDataJson.scala).
The executable contract is [StructuredDataTypeSpec.scala](../../src/test/scala/org/goldenport/schema/StructuredDataTypeSpec.scala).
Explicit external format selection is declared in
[StructuredDataFormat.scala](../../src/main/scala/org/goldenport/schema/StructuredDataFormat.scala),
implemented in [StructuredDataText.scala](../../src/main/scala/org/goldenport/schema/StructuredDataText.scala),
with package-private HOCON support in
[StructuredDataHocon.scala](../../src/main/scala/org/goldenport/schema/StructuredDataHocon.scala),
and specified by [StructuredDataTextSpec.scala](../../src/test/scala/org/goldenport/schema/StructuredDataTextSpec.scala).
Canonical typed consumers use [ValueReader.scala](../../src/main/scala/org/goldenport/convert/ValueReader.scala),
specified by [StructuredValueReaderSpec.scala](../../src/test/scala/org/goldenport/convert/StructuredValueReaderSpec.scala).
The connected route is specified by [record-normalization.md](record-normalization.md)
and [RecordNormalizationSpec.scala](../../src/test/scala/org/goldenport/record/RecordNormalizationSpec.scala).

## R1. Canonical identities

`XJson` MUST be a case object in the existing `DataType` hierarchy implementing
`CanonicalDataType[io.circe.Json]`, with stable name `json`.
`XSemiStructuredData` MUST be a distinct case object implementing
`CanonicalDataType[org.goldenport.record.Record]`, with stable name
`semiStructuredData`. Consumers MUST use these actual public identities.

## R2. JSON values and explicit text conversion

`XJson.normalizer.normalize(value: Any): Consequence[Json]` MUST accept existing
`Json` values unchanged, including objects, arrays, strings, booleans, numbers
and `Json.Null`. Other runtime values, including `String` and `Record`, MUST
produce `Consequence.valueInvalid` with datatype `XJson`.

`XJson.fromJsonText(text: String): Consequence[Json]` MUST explicitly parse every
JSON shape. Malformed or absent text MUST produce `Consequence.valueFormatError`
with datatype `XJson`. Normalization MUST NOT infer parsing from a String or
infer a SQL/provider mapping from a runtime shape.

## R3. Semi-structured root contract

`XSemiStructuredData.normalizer.normalize(value: Any): Consequence[Record]`
MUST accept a supported `Record` or object-shaped `Json`. A Record MUST be
normalized by direct semantic traversal of its fields and nested values, without
serializing to JSON and decoding again for validation. Arrays, scalar JSON values, `Json.Null`, String and unsupported
application objects at the root MUST produce `Consequence.valueInvalid` with
datatype `XSemiStructuredData`.

Explicit public conversions are:

```scala
XSemiStructuredData.fromJson(value: Json): Consequence[Record]
XSemiStructuredData.toJson(value: Record): Consequence[Json]
```

`fromJson` MUST accept only JSON objects. `toJson` MUST validate the supplied
Record through the shared semantic normalizer and encode its canonical values as
object-shaped Json. An absent root is invalid.

## R4. Preserved structure

JSON objects MUST become nested `Record` values constructed with
`Record.createFull`. Null-valued object attributes MUST be omitted before recursive
child decoding; native-null input attributes MUST be omitted at Record's shared
construction boundary. This rule applies at every object depth, including objects in arrays.
Arrays MUST become `Vector` values. Null array/sequence elements at any depth MUST
fail as invalid values; they MUST NOT be filtered out or shift subsequent positions.
Strings (including `"null"` and empty String), booleans (including false) and zero
MUST retain their meaning. Empty root and nested objects and empty arrays MUST remain
present, including a child object emptied by null-attribute omission.

Record normalization MUST support nested Records, Scala sequences including Vector,
native primitive and reference arrays, Cats `NonEmptyVector`, String and Boolean.
Canonical semantic normalization converts supported sequences to ordered Vectors;
construction itself retains an actual NonEmptyVector's shape while owning its
elements. Untyped decoding MUST NOT
infer a NonEmptyVector from array length. Retained object key order and array element
order MUST be preserved. Conversion MUST read Record fields directly. Ordinary
RecordEncoder JSON/YAML output MUST use the same canonical semantic contract.

## R5. Exact numeric meaning

JSON numbers MUST become exact Scala BigDecimals. If an exact BigDecimal
representation is unavailable, conversion MUST fail as an invalid
`XSemiStructuredData` value; it MUST NOT fall back to Double.

Record encoding MUST support Scala and Java integral values (Byte, Short, Int,
Long, BigInt and Java BigInteger), Scala and Java BigDecimal, and finite Float
and Double. Floating point inputs MUST use their finite decimal spellings.
Canonical Record normalization MUST convert these supported numeric inputs to
BigDecimal. Numeric meaning MUST survive values beyond Long range and Double
precision. Decimal scale spelling and the original numeric runtime class are
not required to survive canonical normalization.

## R6. Invalid members

Unknown objects, nonfinite Float/Double and duplicate Record keys at any object
level MUST produce `Consequence.valueInvalid` with datatype
`XSemiStructuredData`. Unknown values MUST NOT be converted through `toString`,
omitted or accepted by a general runtime object mapper. Json-valued Record
members are unsupported; callers MUST use the declared nested Record values.

## R7. Failure composition

All public outcomes MUST use existing `Consequence`. Malformed declared JSON
text MUST be distinguished from a wrong shape or wrong runtime value.
Independent recursive fields and array elements MUST aggregate failures through
`Consequence.zipN`. Dependent encoding/decoding steps MAY use `flatMap`.
Duplicate-key validation and field validation MUST retain independent failures.

## R8. Executable roundtrip properties

The executable specification MUST generate exact decimal numbers, Unicode
strings, nested objects, nullable object attributes and non-null array elements.
Json-to-Record-to-Json MUST equal an independently computed recursive omission of
null object attributes, with retained key and element order. Record roundtrips MUST
equal the normalized value, not an original null-bearing input. Canonical semantic
normalization MUST be idempotent. Expected property values MUST come from a pure
independent oracle, not production normalizers. The specification
MUST also cover empty values, wrong roots, duplicate keys, unknown objects,
nonfinite numbers and recursive failure aggregation with Given/When/Then
adjacent to setup, interpretation and expectations.

## R9. Existing scalar compatibility and coordinated Phase status

Existing scalar datatype identities, names, signatures and normalizer behavior
MUST retain their established semantics. Phase 3 / RCA-01-02A changes Record
create/createFull/data factory omission and empty preservation, getVector collection
support and RecordEncoder JSON/YAML canonical conversion. RCA-03 extends the
specified policy to constructor/copy/dataAuto, ordinary decoding/source loading
and the existing XML collection surface as detailed in R13. Its authored
scenarios do not extend the earlier Step's validation evidence.

This accumulator specifies canonical types and explicit JSON/YAML/HOCON conversion.
Generated schema metadata in SimpleModeler/Cozy and SQL mapping/DataStore
integration remain upcoming requirements in the coordinated Phase. External
format selection MUST remain separate from SQL JSON text storage. This document
and authored executable scenarios do not establish final Phase acceptance,
generator acceptance, validation completion or release status.

## R10. Explicit external formats

The public `org.goldenport.schema.StructuredDataFormat` enum MUST contain
exactly `Json`, `Yaml` and `Hocon`. The public API is:

```scala
XSemiStructuredData.fromText(text: String, format: StructuredDataFormat): Consequence[Record]
```

The selected format alone MUST determine parsing. String aliases, defaults,
format inference, path-based selection and fallback to another syntax are
unsupported. Successful conversion MUST satisfy the existing canonical
Record/JSON contract, including recursive null-attribute omission, invalid null
array elements, retained empty objects and arrays,
Unicode, exact BigDecimal values and nested Vector/Record representations.
Undecoded String values MUST remain invalid for the canonical normalizer.

JSON parsing MUST call the canonical `fromJson` object conversion after syntax
parsing. YAML MUST compose a single SnakeYAML 2.1 Node graph without constructing
Java application objects or invoking the ordinary Record decoder. The root MUST
be a `MappingNode` with `Tag.MAP`. Mappings MUST become `Record.createFull`
values, and sequences MUST become Vectors. Only MAP, SEQ, STR, BOOL, NULL, INT
and FLOAT node tags are supported. Mapping keys MUST be STR scalar nodes;
empty and quoted numeric string keys are supported. Duplicate and merge keys
are invalid. Acyclic alias reuse MUST preserve every member; cycle detection
MUST use node identity along the current path and reject cyclic aliases.

YAML numeric interpretation MUST use original lexemes, remove underscores,
preserve signs and use unlimited precision. Integers support decimal, `0b`
binary, `0x` hexadecimal, YAML 1.1 leading-zero octal and colon-separated
base-60 components. Floats support finite decimal/exponent and base-60 values
with a fractional final component. Base-60 accumulation MUST be exact.
Nonfinite or unrepresentable numbers MUST fail, with no Float/Double fallback.
BOOL nodes support true/false, yes/no and on/off case-insensitively. Quoted
scalars retain their STR meaning regardless of their spelling.

HOCON MUST use `ConfigFactory.parseString` with explicit CONF syntax. Every
include form (plain, file, URL and classpath, including required forms) MUST be
rejected before external IO. The rejecting includer's fallback MUST remain
the same rejecting includer. Conversion MUST NOT load default configuration,
read environment or system properties, or call `resolve`. `config.isResolved`
MUST be true; required, optional and locally referential substitutions are
invalid even when resolution could otherwise supply or omit a value.

HOCON object conversion MUST traverse `config.root()` ConfigObject entries
directly, preserving literal quoted dotted keys separately from nested objects,
arrays and null input syntax. Config lists MUST become Vectors. Strings and booleans
MUST follow their declared ConfigValueType. Null object attributes MUST be omitted
and null list elements MUST fail during direct conversion. NUMBER conversion MUST
use public Config string coercion via
`value.atKey("canonicalValue").getString("canonicalValue")` to retain config
1.4.3 original numeric text, followed by exact BigDecimal conversion with
`MathContext.UNLIMITED`. `unwrapped` Number and rendered numeric text MUST NOT
be used because they can lose precision. Object order follows ConfigObject
iteration; lexical HOCON field order is not a roundtrip requirement.

Config 1.4.3 interprets bare integer lexemes outside Long range as unquoted
strings. The package-private HOCON helper MUST parse the original input first
with the same explicit CONF options and rejecting includer, and reject unresolved
substitutions before adapting that admitted input. It MUST append `.0` only to
complete unquoted integer value tokens matching `-?[0-9]+` whose BigInt is outside
Long range. Quoted and triple-quoted bodies, keys and dotted key paths, comments,
ordinary numbers and other unquoted tokens MUST remain intact. Object and array
lexical contexts MUST distinguish keys from values, retain a pending value
across a newline after `=` or `:`, and recognize native HOCON whitespace.

The adapted input MUST use the identical parser options. Original and adapted
ConfigObjects MUST be paired by exact key set and ConfigLists by length and
index. STRING-to-NUMBER pairs MUST use the adapted exact numeric text;
STRING-to-STRING pairs MUST retain the original string, including concatenations;
NUMBER-to-NUMBER pairs MUST use original exact numeric text. Objects and lists
MUST recurse, agreeing BOOLEAN types MUST retain original values, and agreeing
NULL object members MUST be omitted before recursion. NULL list elements fail.
Unexpected type pairs or key/length differences MUST produce an invalid outcome
without guessing or omitting members. If no token changes, the original root
MUST serve as both sides without another parse. This compatibility adaptation
MUST preserve native object concatenation and duplicate-key precedence, and
MUST NOT introduce public APIs, runtime string-shape inference, internal Config
classes, reflection, additional dependencies or external IO. The existing public
Config coercion and unlimited-precision BigDecimal conversion remain the sole
numeric conversion path, including integer values longer than 309 digits.

## R11. Unsupported external inputs and failure meaning

All results MUST use Consequence with datatype `XSemiStructuredData`. Malformed
syntax, multiple YAML documents, rejected HOCON includes and null text MUST
produce `valueFormatError`. A null format selection, a valid non-object JSON or
YAML root, unresolved HOCON substitutions, unsupported YAML tags/keys/cycles
and invalid numeric conversion MUST produce `valueInvalid`. Empty YAML text
has no mapping root and is invalid. Timestamp, binary, set and custom YAML
tags are unsupported. Invalid diagnostics MUST be concise and MUST NOT
traverse cyclic nodes or copy their payloads.

Parser and numeric exceptions MUST be contained at explicit conversion
boundaries. Null array elements are invalid in JSON, YAML and HOCON at any nesting
depth. Only null object attributes are omitted; other invalid values MUST NOT
become successful empty values, omitted members or strings. Independent child conversion MUST aggregate failures with
`Consequence.zipN`; only dependent parsing, decoding and canonical normalization
may sequence with `flatMap`. Ordinary Record JSON/YAML encoding shares this contract.
Ordinary decoder/source-loading entry points share the conversion rules in R13;
scalar datatype APIs MUST retain their established behavior.

The executable contract MUST cover the supported and rejected inputs above
with adjacent Given/When/Then clauses and should matchers. Generated exact
decimals, Unicode strings, nullable object attributes and non-null nested array
elements serialized as JSON text valid in all three formats MUST preserve independently
normalized semantic JSON meaning through every explicit decoder. HOCON lexical key order MUST NOT be
asserted.

## R12. Canonical typed readers

The `org.goldenport.convert.ValueReader` companion MUST provide the
canonical givens `ValueReader[io.circe.Json]` and
`ValueReader[org.goldenport.record.Record]`. Their public APIs are:

```scala
summon[ValueReader[Json]].readC(value: Any): Consequence[Json]
summon[ValueReader[Record]].readC(value: Any): Consequence[Record]
```

The Json reader MUST delegate directly to `XJson.normalizer.normalize(value)`.
Existing Json values of every shape, including `Json.Null` and exact large
numbers, MUST remain unchanged. String, Record, null runtime roots and other
non-Json values MUST retain the canonical invalid failure with datatype `XJson`.

The Record reader MUST delegate directly to
`XSemiStructuredData.normalizer.normalize(value)`. Supported Records and
object-shaped Json MUST retain canonical numeric, Unicode, null-attribute omission, empty,
nested-object and ordered-array semantics from R3-R8. Undecoded String,
non-object JSON, null runtime roots and unsupported Record members MUST retain
canonical invalid failures with datatype `XSemiStructuredData`, including
aggregation of independent member failures.

The companion MUST also provide a generic
`given [A](using ValueReader[A]): ValueReader[cats.data.NonEmptyVector[A]]`.
It MUST accept actual NonEmptyVector, Seq and native Array inputs, preserving
element order and invoking the existing element `readC`. Empty sequences and all
other runtime shapes, including String, MUST fail with structured
`Consequence.valueInvalid(message)`. Native-null elements MUST fail before invoking
even an `Any` element reader. Independent element failures MUST aggregate with
`Consequence.zipN`. Its `readContextC` MUST pass the supplied ExecutionContext to
every element reader; it MUST NOT silently fall back to context-free reads.

These readers MUST NOT infer a format, parse String, add conversion fallbacks,
select a SQL/provider mapping, add datatype-name registries or introduce context policy. Existing `read`,
`readC` and `readContextC` signatures and defaults MUST remain unchanged.
Optional `read` MUST continue to return `None` on canonical failure. Existing
scalar readers MUST retain their behavior; `ValueReader[String]` MUST preserve
the original lexical JSON-looking text.

SimpleModeler generated `_record_get_as_c[A]` consumers require a
`ValueReader[A]`; the Json and Record givens MUST support this generic typed
dependency through the companion's implicit scope. Authored Core readers do
not establish generator, DataStore or final Phase acceptance. Explicit
external decoding and storage decisions remain separate consumer obligations.

The executable contract MUST exercise the actual generic typed reader API with
adjacent Given/When/Then clauses and should matchers. Generated canonical object
Json containing exact decimals, Unicode, nested objects, nullable attributes and
non-null arrays MUST preserve normalized semantic JSON equality through the Record reader followed by explicit
`XSemiStructuredData.toJson`. The actual combined Core validation and independent
review results are recorded in Stage Status.

## R13. Record construction and ordinary IO entry points

The complete entry-point contract and CNCF migration input are specified in
[record-normalization.md](record-normalization.md), with executable scenarios in
[RecordEntryPointSpec.scala](../../src/test/scala/org/goldenport/record/RecordEntryPointSpec.scala).
Record remains the same field-vector case class with Product, equality and
extractor behavior. Its primary constructor is private; public APIs are:

```scala
Record(fields: Vector[Field] = Vector.empty): Record
Record.fromProduct(product: Product): Record
Record.fromFieldsC(fields: Vector[Field]): Consequence[Record]
Record.createC(xs: Seq[(String, Any)]): Consequence[Record]
record.copy(fields: Vector[Field] = record.fields): Record
```

fromFieldsC is the sole construction kernel. Absent vectors, null Fields, null
keys and absent or unsupported Field.Value inputs fail with
Consequence.valueInvalid(message). Single(null) attributes are omitted before
entering normal logic. Seq and native Array values are recursively copied to
immutable Vectors; actual NonEmptyVectors retain their shape with owned elements.
Null elements fail at every depth with zipN aggregation. Nested Records, empty
structures, order, duplicate fields and present scalar/opaque atoms are retained.
Construction does not canonicalize numbers or inspect arbitrary models;
XSemiStructuredData separately validates canonical numbers, unsupported values
and duplicate keys. No semantic validation uses a JSON roundtrip.

All public create/createFull/Map/data/dataOption/dataAuto/apply/copy and immutable
append/replacement/concatenation/filter routes reach this kernel. Convenience
APIs TAKE the result; C paths preserve the original failure taxonomy. The existing
public fromProduct adapter, called directly or through Mirror.ProductOf[Record],
MUST pass the Product's first element cast as Vector[Field] through apply and
the same kernel, retaining existing Product shape and cast behavior. createC
MUST return Consequence.valueInvalid(message) for an absent input sequence or
native-null tuple, aggregating tuple admission with zipN before fromFieldsC.
dataAuto
retains empty structures after its existing Some/None unwrapping; None and
Some(null) are omitted. Null append preserves existing fields; null replacement
removes the replaced key. Internal callers use removeKeys. Getters and path/string
access consume the invariant without routine null filtering, with their existing
typed-reader, missing-value and path-precedence semantics.

StructuredDataJson, StructuredDataText and StructuredDataHocon are shared only
inside private[goldenport] scope. Ordinary RecordDecoder JSON/YAML single APIs
require objects; plural APIs require arrays/lists of objects and auto APIs also
accept one object. Explicit empty lists succeed. Empty YAML documents, scalar
and null roots fail in plural as well as single/auto APIs. This deliberately
removes the old yamlRecords(null/empty-document)-to-empty-vector ambiguity.
String/Bag/InputStream wrappers retain their public forms. Numeric values are
exact BigDecimals. SnakeYAML compose handles its absent Node through Option;
mapping keys and duplicates are validated before Tag.NULL members are omitted,
and Tag.NULL sequence elements fail. Existing exact numeric, boolean, alias-cycle,
unsupported-tag, merge-key and non-string-key restrictions remain in force.

Strict literal HOCON retains all R10 restrictions and unlimited exact numeric
token adaptation. The internal fromConfig(Config): Consequence[Record] converts
an already parsed/resolved root directly using that root as both original and
adapted objects; it performs no IO, resolution or numeric-looking-string inference.
Ordinary source loading and HOCON import retain their existing
ConfigFactory.parseString(...).resolve() features before calling this converter.
Empty child objects, literal dotted keys and recursively converted list objects
survive. NULL object members are omitted before recursion; NULL list elements
fail. Import preserves first-nonempty records/items/data priority, scalar value
wrapping and whole-root fallback. Existing schema coercion still returns Int20
for age under XInt. TOML foreign maps/collections are converted recursively at
ingress with Option member omission, invalid null list positions and zipN
aggregation, preserving existing scalar/time/table features.

Row JSON and TSL exports aggregate canonical per-record JSON and render arrays.
YAML uses canonical per-record YAML with deterministic list indentation, `[]` for
an empty collection and a valid empty mapping item for Record.empty. HOCON renders
the same canonical JSON objects in the existing records list and MUST recursively
emit integral numbers outside Long range as exact unquoted decimal numeric tokens
with scale 1 so ordinary Config loading preserves numeric type. This emission
MUST NOT infer numeric Strings or change full-config resolution, strict literal
parsing or public APIs. These paths return
structured failures directly. Nested Record, NonEmptyVector, Seq/Array text cells
share internal valueToJson semantic conversion; missing CSV/Excel cells remain
Option until rendered as empty String. Ordinary scalar table display and export
schemas, content types and extensions are unchanged. XML display traverses
NonEmptyVector with the existing item shape, escaping and scalar lexical display;
it does not impose canonical structured-type validation on every XML scalar.

This constructor change affects source/ABI compatibility: callers must migrate
from `new Record` and anonymous subclasses to Record(...), Record.fromFieldsC or
copy. CNCF `DurableJobOperationResponseCodecSpec.scala:115` uses a hostile
`new Record(Vector.empty)` anonymous subclass, and lines 92-145 intentionally
construct malformed null fields. CNCF115 must redesign these closed-codec
fixtures without invalid Record instances or subclass construction. This is a
concrete downstream handoff, not downstream validation, publication or a
compatibility-transparency claim. XJson raw JSON/null semantics and all earlier
evidence remain historical and unchanged. Step/Phase acceptance still requires
the separately recorded actual validation and independent review outcomes.
