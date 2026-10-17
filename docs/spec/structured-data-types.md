# Canonical structured datatypes

Status: Normative combined Core canonical/external-format/typed-reader accumulator for
PHASE-1 / STEP-2 / PREREQ-CORE-A, PREREQ-CORE-B and PREREQ-CORE-C. The combined prerequisite
passed 94/94 focused tests across six suites and the complete Core protected focused
independent conformance review (PASS). Local dependency publication, generator/DataStore
integration, final full validation and Phase closure remain pending.

## Stage Status

- Current status: Core canonical, explicit JSON/YAML/HOCON and typed-reader APIs are authored;
  the combined accumulator passed 94/94 focused tests across six suites and the complete
  Core protected focused independent conformance review (PASS). Local dependency publication,
  generator/DataStore integration, final full validation and Phase closure remain pending.
- Owner: goldenport-core, coordinated from Workbench PHASE-1 / STEP-2.
- Update rule: Advance this status from actual focused validation and independent
  Core review results. Generator, DataStore and final Phase acceptance retain
  their separate closure criteria.

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
validated through JSON encoding and then normalized by the same JSON-to-Record
conversion. Arrays, scalar JSON values, `Json.Null`, String and unsupported
application objects at the root MUST produce `Consequence.valueInvalid` with
datatype `XSemiStructuredData`.

Explicit public conversions are:

```scala
XSemiStructuredData.fromJson(value: Json): Consequence[Record]
XSemiStructuredData.toJson(value: Record): Consequence[Json]
```

`fromJson` MUST accept only JSON objects. `toJson` MUST validate the supplied
Record and return an object-shaped Json. An absent root is invalid.

## R4. Preserved structure

JSON objects MUST become nested `Record` values constructed with
`Record.createFull`. Arrays MUST become `Vector` values. Strings, booleans and
explicit null members MUST retain their meaning. Empty root and nested objects,
empty arrays and null-valued fields MUST remain present.

Record encoding MUST support nested Records, Scala sequences including Vector,
native primitive and reference arrays, null, String and Boolean. Object key
order and array element order MUST be retained. Conversion MUST read Record
fields directly rather than using omission-oriented maps or constructors.

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
strings, nested objects, arrays and null values. Supported object-shaped JSON
MUST preserve semantic meaning and key order across Json-to-Record-to-Json
conversion. Canonical Record normalization MUST be stable. The specification
MUST also cover empty values, wrong roots, duplicate keys, unknown objects,
nonfinite numbers and recursive failure aggregation with Given/When/Then
adjacent to setup, interpretation and expectations.

## R9. Existing scalar compatibility and coordinated Phase status

Existing scalar datatype identities, names, signatures and normalizer behavior
MUST retain their established semantics. Existing RecordDecoder, RecordEncoder,
RecordSourceLoader and Record constructors are outside this Slice's edits.

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
Record/JSON contract, including explicit null, empty objects and arrays,
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
arrays and explicit null. Config lists MUST become Vectors. Strings, booleans
and null MUST follow their declared ConfigValueType. NUMBER conversion MUST
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
MUST recurse, and agreeing BOOLEAN and NULL types MUST retain original values.
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
boundaries. Invalid values MUST NOT become successful empty values, omitted
members or strings. Independent child conversion MUST aggregate failures with
`Consequence.zipN`; only dependent parsing, decoding and canonical normalization
may sequence with `flatMap`. Existing Record source loading, ordinary encoding
and decoding and scalar datatype APIs MUST retain their legacy behavior.

The executable contract MUST cover the supported and rejected inputs above
with adjacent Given/When/Then clauses and should matchers. Generated exact
decimals, Unicode strings, null and nested object/array values serialized as
JSON text valid in all three formats MUST preserve canonical semantic JSON
meaning through every explicit decoder. HOCON lexical key order MUST NOT be
asserted.

## R12. Canonical typed readers

The `org.goldenport.convert.ValueReader` companion MUST provide exactly the
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
object-shaped Json MUST retain canonical numeric, Unicode, null, empty,
nested-object and ordered-array semantics from R3-R8. Undecoded String,
non-object JSON, null runtime roots and unsupported Record members MUST retain
canonical invalid failures with datatype `XSemiStructuredData`, including
aggregation of independent member failures.

These readers MUST NOT infer a format, parse String, add conversion fallbacks,
select a SQL/provider mapping or introduce context policy. Existing `read`,
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
Json containing exact decimals, Unicode, nested objects, arrays and null MUST
preserve semantic JSON equality through the Record reader followed by explicit
`XSemiStructuredData.toJson`. Combined Core validation and independent review
remain pending as recorded in Stage Status.
