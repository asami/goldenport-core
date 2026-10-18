# Typed numeric code codec

Date: 2026-10-19
Status: normative contract

## Scope and ownership

`org.goldenport.convert.NumericCodeCodec[A]` defines a typed, bidirectional
correspondence between a value and an explicitly assigned `Int` code. Core owns
this generic contract. The value's owning model supplies the correspondence;
Core does not depend on models, SQL, providers or code generators.

```scala
trait NumericCodeCodec[A] {
  def encodeC(value: A): Consequence[Int]
  def decodeC(code: Int): Consequence[A]
  final def jsonCodec: io.circe.Codec[A]
}
```

The companion exposes:

```scala
def instance[A](
  codeOf: A => Option[Int],
  valueOf: Int => Option[A]
): NumericCodeCodec[A]

def jsonCodec[A](codec: NumericCodeCodec[A]): io.circe.Codec[A]
```

## Explicit correspondence

The owning declaration MUST provide an injective fixed correspondence for all
values admitted to numeric persistence. For every supported value `v`,
`decodeC(encodeC(v))` returns `v`; for every supported code `c`,
`encodeC(decodeC(c))` returns `c`, composing through `Consequence`.

`instance` delegates to the supplied forward and inverse callbacks. A missing
forward assignment or unknown inverse code returns the existing structured
`Consequence.valueInvalid` failure. Callback exceptions are contained by the
`Consequence` boundary. Failure MUST NOT become a successful `None`, default
value or raw code.

Every `Int`, including zero, negative numbers, `Int.MinValue` and `Int.MaxValue`,
can be an explicit code. Zero has no implicit absence meaning. Optional absence
belongs to the surrounding Option/storage contract.

Codes MUST NOT be inferred from ordinal, declaration order, labels, names,
reflection, a global registry or arbitrary `toString`. Reordering declarations
or changing presentation labels MUST preserve the explicit correspondence.
Validation of complete and unique model declarations belongs to their producer;
`instance` does not discover a value universe or maintain assignment history.

## Strict numeric JSON adapter

Both the trait's `jsonCodec` and the companion's `jsonCodec(codec)` select the
same numeric representation:

- Encoding invokes `encodeC` and emits the successful code using `Json.fromInt`.
  A failed conversion crosses Circe's total encoder interface through
  `Consequence.TAKE`; it MUST NOT emit a default, JSON null or symbolic string.
- Decoding accepts only actual JSON numbers whose mathematical value is exactly
  integral and within the Int range. Integral decimal and exponent notation are
  accepted. Fractional or out-of-range numbers, numeric strings, objects, arrays,
  booleans and null are rejected before invoking `decodeC`.
- A valid numeric input is passed to the typed `decodeC`. Unknown codes and
  callback failures become Circe decoding failures using the structured
  conclusion's standard `print` rendering and the current cursor history.
  Callback invocation remains inside the `Consequence` exception boundary.

The adapter MUST NOT round, truncate or pass through a floating-point
intermediate to obtain a code.

## Generated type binding and presentation separation

A generated POWERTYPE owns its instance using its explicit mapping and inverse:

```scala
given numericCodeCodec: NumericCodeCodec[Value] =
  NumericCodeCodec.instance(v => dbValueOf(v.value), fromDbValue)
```

The declared persistence binding selects
`Value.numericCodeCodec.jsonCodec` while the typed value is still available.
The type's external JSON/CLI projection can retain its symbolic text. Generic
`ExternalValueEncoder` and model external projection do not select or infer
numeric persistence codecs after type information has been erased.

This contract introduces no SQL/provider dependency, per-type Core registry,
new external-value branch, database migration or StateMachine behavior. Existing
stored representations require a separately authorized migration contract.

## Executable specification

[NumericCodeCodecSpec](../../src/test/scala/org/goldenport/convert/NumericCodeCodecSpec.scala)
defines these obligations:

| Behavior | Required evidence |
| --- | --- |
| Explicit encode/decode and numeric JSON correspondence | At least 100 generated injective mapping samples, each including zero, negative codes and both Int boundaries |
| Declaration and label stability | At least 100 generated reversed and relabelled mappings retain their assigned codes |
| Missing/unknown assignment | Structured value-invalid failures, with no default or successful absence |
| Foreign callback containment | Forward and inverse exceptions remain structured failures; arbitrary string presentation is not consulted |
| Exact JSON integer admission | Integral decimal/exponent numbers decode to the assigned value; at least 100 generated fractional/overflow samples are rejected |
| Non-number JSON rejection | Strings, objects, arrays, booleans and null never reach the inverse callback |
| JSON failure adaptation | Encoding escalates through TAKE; decoding retains the structured rendering and Circe cursor history |

This specification states obligations and makes no execution or acceptance claim.
