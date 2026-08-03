# Phase 55 GCF-01 Generic Configuration Inventory

Date: 2026-08-02

Status: frozen implementation inventory

## Current authority

`org.goldenport.configuration` currently represents effective values as
`Configuration(values: Map[String, ConfigurationValue])`; `get`, `getString`,
`getBoolean`, and `getNumber` therefore remain String-keyed lookup surfaces.
`ResolvedConfiguration` pairs that map with a separately mutable-in-construction
`ConfigurationTrace`. `ConfigurationResolver` loads admitted sources and folds
them through `MergePolicy`; the current `MergePolicy.merge` loads its source
again, so the same physical source can be read twice during one resolution.

`ConfigurationSource` currently supplies system-property, environment,
argument, and file contributions. `ConfigurationTrace` and its printer carry
the winning `ConfigurationValue` and source entry separately from the effective
map. These are the GCF-02 failing-first boundaries; this inventory makes no
runtime change.

## Frozen generic ownership

`simplemodeling-lib` owns these target-parametrized, namespace-neutral public
abstractions:

- `CanonicalParameterId`, `ConfigurationParameter[A]`, and
  `ConfigurationValueCodec[A]`;
- `ConfigurationProvenance`, `ConfigurationBindingCandidate[A, T]`,
  `ConfigurationBindingCandidates[T]`, `ConfigurationBinding[A, T]`, and
  `ConfigurationBindingCollection[T]`;
- source snapshot loading, runtime-assigned rank and ordinal, deterministic
  generic resolution, typed lookup, and trace/diagnostic projection.

Generic code does not define Textus/CNCF targets or their identities, parameter
meanings, namespace policy, source admission, or aliases. Those are catalog
concerns in the CNCF repository. Heterogeneous storage is private; public lookup accepts
the exact `ConfigurationParameter[A]` witness and returns only `A` (or its
typed binding).

## Frozen invariants

- Candidates never have `overridden`; only an effective binding links directly
  to the immediately preceding effective binding for the same parameter/type.
- Candidates are one immutable, one-load-per-physical-source snapshot. A
  resolved collection has at most one winner for each canonical parameter.
- Source rank dominates across sources. Within one source, target specificity
  is `Global < ComponentClass < SubsystemInstance < ComponentInstance`.
- Rejected, malformed, duplicate, and context-ineligible candidates are not
  override history.
- The runtime chain is complete and immutable. External diagnostics project at
  most 16 newest entries; source identity is at most 256 characters and a
  truncation count is recorded. Confidential effective and historical values
  are redacted.

## GCF-02 failing-first cases

GCF-02 must first prove type coupling, target/identity validation, candidate
multiplicity, one winner per parameter, source and target ordering, direct
acyclic history, duplicate rejection, single physical-source load, exact typed
lookup, and redaction/projection bounds. Existing resolver/trace specifications
are regression evidence, not proof of the new model.
