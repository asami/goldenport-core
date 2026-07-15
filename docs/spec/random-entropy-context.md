# Random and Entropy Context

status=stable
scope=core
audience=core / CNCF / infra / component developers

## Purpose

`RandomContext` provides domain and operational pseudo-random values.
`EntropyContext` provides opaque bytes and URL-safe tokens. They are separate
execution capabilities so that identifier or security entropy never advances a
business-random sequence.

## RandomContext Contract

A random context provides scalar values and named streams:

```scala
trait RandomContext {
  def stream(purpose: String): RandomContext
  def nextInt(): Int
  def nextInt(bound: Int): Int
  def nextLong(): Long
  def nextDouble(): Double
  def nextBoolean(): Boolean
}
```

`RandomContext.system()` uses a production random provider.
`RandomContext.seeded(seed)` is reproducible. Two seeded roots with the same
seed produce the same advancing sequence for the same normalized purpose path.
Calls made through one purpose stream MUST NOT advance another purpose stream.
Repeated lookup of one purpose returns that stream's continuing sequence rather
than restarting it.

A purpose is a stable, low-cardinality semantic path, not a request ID, entity ID,
or other per-call value. A higher layer combines invocation identity with the root
seed before constructing the context. One stream is thread-safe, but concurrent
caller ordering is not by itself replayable; a controlled runtime must serialize
or reject that execution shape.

Purpose names are trimmed, case-normalized, and restricted to letters, digits,
period, hyphen, and underscore. Other characters normalize to underscore. A
blank purpose and a non-positive integer bound are invalid arguments.

`RandomContext.from("fixed")` and `from("deterministic")` preserve the legacy
constant-value context. `from("system")` and `from("random")` select a system
context. Unknown names retain the legacy fixed fallback until configuration
parsing is migrated to a structured result.

## EntropyContext Contract

An entropy context provides opaque data by purpose:

```scala
trait EntropyContext {
  def bytes(purpose: String, size: Int): Array[Byte]
  def token(purpose: String, size: Int): String
}
```

`size` is the requested byte count. `token` encodes those bytes as unpadded,
URL-safe Base64. A negative size or blank purpose is invalid.

`EntropyContext.secure()` is the production default.
`EntropyContext.deterministic(seed)` exists for explicitly controlled tests.
Deterministic entropy has the same purpose isolation and advancing-sequence
semantics as seeded random streams.

Selecting seeded domain randomness MUST NOT select deterministic entropy.
Callers construct and inject the two capabilities independently. Security
entropy remains production-safe unless an explicit test bootstrap supplies a
deterministic entropy context.

## ExecutionContext Composition

`ExecutionContext.Core` carries both `random` and `entropy`. Bootstrap code MUST
construct them explicitly. Core logic MUST NOT instantiate JVM random or
security-random providers directly.

Recommended purpose families are:

- `domain.*` for business decisions;
- `retry.*` for operational jitter;
- `id.*` for identifier generation;
- `security.*` for security-owned entropy.

Seed material and generated entropy MUST NOT appear in `toString`, diagnostics,
logging, metrics, or introspection.

## Executable Specifications

- `org.goldenport.context.RandomContextSpec`
- `org.goldenport.context.EntropyContextSpec`
- `org.goldenport.context.ExecutionContextSpec`
