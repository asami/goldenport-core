# Exact JSON integer reads

This contract governs public `ValueReader[Int].readC` and
`ValueReader[Long].readC`, including ordinary `Record.getAsC` and generated
business readers that consume strict JSON Records.

## R1: Exact integral decimal acceptance

The Int and Long readers accept Scala `BigDecimal` values that are mathematically
whole and within the target range. Zero, negative values, scaled integral
decimals and integral exponent notation return the exact target integer.

## R2: Exact target ranges and precision

Int acceptance includes `Int.MinValue` through `Int.MaxValue`; Long acceptance
includes `Long.MinValue` through `Long.MaxValue`. Comparisons occur in decimal
precision before conversion. Long integers beyond the exact Double range,
including `9007199254740993`, retain their value. Floating intermediates,
rounding and truncation before the range/integrality checks are forbidden.

## R3: Structured rejection

Fractional decimals and integral decimals outside the target range return
`Consequence.Failure` through the existing `valueInvalid` contract, using `XInt`
or `XLong` respectively. Malformed or decimal integer strings and unsupported
inputs, including Double, Float, Boolean and null, remain failures. Java decimal
and BigInt inputs gain no new acceptance. Rejection does not throw or supply a
default integer. Malformed JSON remains a decoder failure.

## R4: Existing input behavior

Native Int and range-valid native Long inputs retain their Int behavior. Native
Long and Int inputs retain their Long behavior. Trimmed integer strings retain
their exact target parsing and rejection rules. Other readers, signatures,
implicit identities and failure tags retain their existing behavior.

## R5: Strict Record intake

`RecordDecoder.json` preserves JSON number precision as Scala `BigDecimal`.
The ordinary `Record.getAsC[Int/Long]` and generated reader path consumes that
number without changing fixture fields into strings or native integers.
Successful present fields return the exact `Some` integer; missing fields
remain `None`. Valid JSON fractions and out-of-range numbers fail integer reads.

## Executable specification mapping

`src/test/scala/org/goldenport/convert/ValueReaderSpec.scala` binds these semantic
examples with visible afterWord metadata and adjacent Given/When/Then.

| Example | Rules | Observable contract |
| --- | --- | --- |
| N1 | R1,R2,R4 | Generated signed Int decimals and explicit boundary/scaled/exponent rows read exactly; native and trimmed string inputs retain behavior |
| N2 | R1,R2,R4 | Generated signed Long decimals, limits and values beyond 2^53 read exactly; native and trimmed string inputs retain behavior |
| N3 | R2,R3 | Both readers reject fractions, out-of-range decimals, malformed strings and unsupported inputs without defaulting or throwing |
| N4 | R1,R2,R3,R5 | Strict JSON Record intake preserves exact zero/limits/large integers, rejects invalid integer reads, and retains missing/malformed behavior |

This mapping defines obligations and makes no execution or acceptance claim.
