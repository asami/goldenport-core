# Typed Configuration Binding Specification

Status: normative generic specification (Phase 55 GCF-10A promotion)

## Scope

The specification covers typed parameter witnesses, strict value codecs,
immutable source snapshots, candidate admission, deterministic resolution,
effective collections, override history, and sanitized trace projection. It does
not specify how a product finds files or names its parameters.

## Required behavior

1. A parameter witness consists of a valid `CanonicalParameterId` and a strict
   `ConfigurationValueCodec[A]`. `decode` rejects incompatible raw value forms;
   `encode` is the only route used to verify a typed value can be represented.
2. A candidate requires the original parameter witness, a non-null exact target,
   a non-null typed value, and valid provenance. Candidate construction fails
   structurally when any requirement is missing.
3. Source admission loads each physical source once. A
   `ConfigurationSourceSnapshot` is immutable and carries source rank, ordinal,
   collision domain, origin, layer, identity, and the loaded value.
4. Candidate construction derives complete provenance from the immutable source
   snapshot and records the input spelling/path and bounded provenance. It does
   not retain or reread the snapshot.
5. Candidate batches reject duplicate canonical parameter/target identities in
   one collision domain. Equivalent canonical and compatibility spellings must
   be normalized or rejected by the owning catalog before admission.
6. Resolution accepts a caller-supplied ordered target context. Within one
   source, the highest-specificity eligible target must be the sole winner;
   ties fail. Across sources, `(sourceRank, sourceOrdinal)` determines the
   winner order. Every later winner records the previous effective binding in
   `overridden`.
7. A resolved collection has one effective binding per canonical parameter id.
   `binding(parameter)` and `value(parameter)` require the original witness and
   fail on witness substitution, even when ids are equal.
8. `ConfigurationBindingTrace.from(collection)` derives entries from the
   effective value and its immutable override chain. It is bounded, deterministic,
   and sanitized: confidential entries are `Redacted`, source identity is
   bounded, and no diagnostic operation loads or opens a resource.

## Compatibility projection

`ResolvedConfiguration` and its legacy trace remain a raw source-resolution
compatibility projection. They may be used before typed admission or at an
explicit compatibility boundary, but they MUST NOT be described as the final
typed authority and MUST NOT be passed to consumers that can receive a narrow
value-only projection.

## Conformance scenarios

An implementation conforms when executable specifications demonstrate:

- strict codec rejection and round-trip encoding;
- one-load source snapshot behavior;
- candidate/effective collection separation;
- duplicate and same-source collision rejection;
- deterministic target/source precedence and complete immutable override chains;
- original-witness lookup enforcement;
- trace derivation from the effective collection;
- bounded provenance and confidential redaction; and
- diagnostics that cannot reload or open resources.

Product repositories add their own catalog, alias, target, default, and
value-only projection scenarios against this generic contract.

## Executable evidence

The generic contract is exercised by these repository-local Executable
Specifications:

- [ConfigurationBindingCoreSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingCoreSpec.scala)
- [ConfigurationSourceSnapshotSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationSourceSnapshotSpec.scala)
- [ConfigurationBindingCandidateConstructorSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingCandidateConstructorSpec.scala)
- [ConfigurationBindingResolverSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingResolverSpec.scala)
- [ConfigurationBindingTraceSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingTraceSpec.scala)
- [ConfigurationBindingStringCodecSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingStringCodecSpec.scala)
- [ConfigurationBindingContractSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationBindingContractSpec.scala)
- [ConfigurationResolutionSnapshotSpec](../../src/test/scala/org/goldenport/configuration/ConfigurationResolutionSnapshotSpec.scala)
