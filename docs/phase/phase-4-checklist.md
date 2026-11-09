# Phase 4 checklist — Type-safe equality and identity collection APIs

status=planned

Plan: [Phase 4](phase-4.md).
Owner: goldenport-core development.
The original delivery IDs and criteria are preserved. All items remain OPEN.

## Stage: Producer delivery

Stage Status:
- Current status: OPEN
- Owner: goldenport-core development
- Update rule: Record actual API, specification, validation and handoff acceptance.
- Closure basis: CORE-01–CORE-05 below; no implementation is claimed.

- [ ] CORE-01: Specify and fix a minimal API using existing Eq/identity contracts;
      distinguish value, reference and domain-ID comparison without inferring
      intent from a member named id. Publish exact signatures/imports.
- [ ] CORE-02: Prove by positive/negative compilation that receiver types cannot
      widen to Any/common parents to admit sibling ID/element types, including
      generic Eq instances. A bare Eq[A] parameter is not sufficient evidence.
- [ ] CORE-03: Qualify actual result order, selected duplicate/collision semantics,
      missing IDs, nested arrays and equality laws with behavioral/property
      specifications. Any supported hashing/ordering operation must use coherent
      Eq/hash/Ordering; do not claim ordinary Set/Map uses the custom instance.
- [ ] CORE-04: Validate the affected Core scope under the qualified Scala 3
      compiler policy and independently review the API. No blanket universal
      Eq/CanEqual, unchecked cast or unrelated whole-library migration.
- [ ] CORE-05: Return SLP-CORE with revision, supported Scala/Cats versions,
      actual API/imports, results/review and ordinary publication/source-selection
      instructions. Identify unsupported operations and migration limitations.
