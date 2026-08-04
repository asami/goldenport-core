# Typed Configuration Binding Design

Status: normative implementation design (Phase 55 GCF-10A promotion)

This document defines the generic binding boundary implemented in
`org.goldenport.configuration`. It is deliberately independent of Textus or
CNCF parameter names and target taxonomies.

## Authority split

`ConfigurationResolver.resolveSnapshot` owns raw source loading and compatibility
projection. It loads each selected physical source once and returns an immutable
`ConfigurationResolutionSnapshot` containing the existing `ResolvedConfiguration`
projection and retained per-source snapshots. `ResolvedConfiguration` is useful
for compatibility and source-resolution consumers; it is not the final typed
configuration authority.

Typed authority starts only after a caller supplies a closed catalog of
`ConfigurationParameter[A]` witnesses and admits source values as
`ConfigurationBindingCandidate` instances. The generic library does not discover
files, infer targets, or define product namespaces.

## Value and identity model

- `CanonicalParameterId` is the validated semantic identity.
- `ConfigurationParameter[A]` couples that identity to the exact
  `ConfigurationValueCodec[A]` witness. Codec decode is strict; it does not
  coerce unrelated `ConfigurationValue` variants.
- `ConfigurationBindingCandidate[A, T]` is an immutable, unresolved value for a
  caller-supplied exact target `T` and complete `ConfigurationProvenance`; the
  generic layer never infers or broadens that target.
- `ConfigurationBinding[A, T]` is an effective value and may point to exactly
  one directly overridden binding, forming an immutable history chain.
- `ConfigurationBindingCandidates[T]` may contain candidates from multiple
  sources and targets. `ConfigurationBindingCollection[T]` contains at most one
  effective binding per canonical parameter identity.

The heterogeneous collection remains private behind the original parameter
witness. Lookup with a different witness having the same textual id fails; a
successful typed lookup therefore cannot silently reinterpret a value.

## Resolution

The caller supplies an ordered `ConfigurationBindingResolutionContext[T]` whose
target vector defines specificity. Resolution:

1. retains only candidates for the requested targets;
2. validates that all candidates for one parameter use the original witness;
3. selects exactly one most-specific target within each source;
4. rejects same-source ties and duplicate canonical parameter/target admission;
5. folds source winners by `(sourceRank, sourceOrdinal)`; and
6. records the previous effective binding as `overridden` on each later winner.

Thus target specificity is source-local, while source precedence is global and
deterministic. No consumer needs to know which target won.

## Provenance and diagnostics

Provenance records origin, layer, stable source identity, optional input path and
spelling, source rank/ordinal, source type, and bounded evidence. Evidence is
limited to 16 entries; diagnostic source identities are bounded to 256 UTF-16
code units without splitting a surrogate pair. Omitted counts are retained.
Confidential values, including overridden confidential history, are redacted by
`ConfigurationBindingTrace.from`; visible values are encoded through the
parameter codec. The trace is derived from the effective collection and cannot
reload or open a resource.

## Boundary rules

String forms are boundary data for files, environment, arguments, launchers,
migrations, and serialized diagnostics. They are not a second internal
authority. Source/resource ownership and discovery remain outside this generic
binding model. Product-specific catalogs, aliases, target admission, defaults,
and value-only consumer projections belong to the adopting repository.

## Non-goals

This design does not define a schema language, source discovery policy,
application semantics, arbitrary qualifier sets, or permanent compatibility
aliases. Those decisions are made by the owning product layer and must be
represented as admitted candidates before generic resolution.
