# Phase 3 - Record Collections and Attribute Normalization

Status: PLANNED
Owner: goldenport-core
Created: 2026-10-18
Checklist: [Phase 3 checklist](phase-3-checklist.md)

## Goal

Make Record the canonical normalized semi-structured representation: support
Cats NonEmptyVector as a first-class collection value, omit null-valued
attributes, and preserve empty structures and the order of retained data.
Deliver one connected construction -> encoding -> decoding -> typed-read
route before completing the remaining affected entry points.

This is a new implementation plan, not a claim that current code conforms.
It does not inherit the unrelated error-model follow-ups of Phase 2.9.

## Ownership and sequencing

Core owns Record, Field, collection conversion, Record codecs, canonical
structured datatype normalization and ValueReader integration. CNCF owns
DataStore/EntityStore adoption in the dedicated
[CNCF Phase 115](../../../cloud-native-component-framework/docs/phase/phase-115.md).
Core must not depend on CNCF or implement SQL/patch policy.

The current [structured datatype specification](../spec/structured-data-types.md)
and its implementation are work in progress originating in Workbench Phase 1
and consumed by CNCF Phase 100. Their null-preserving contract is an explicit
migration input. Preserve their actual work and validation history; do not
silently relabel old evidence as acceptance of the new semantics. When this
phase executes, revise affected specifications and behavior together.

## Planned behavior and acceptance examples

| Input or operation | Required result |
| --- | --- |
| Missing attribute | No attribute |
| Attribute whose value is native null or JSON null during object decoding | Omit that attribute |
| String `"null"` | Retain the String |
| Empty root or child object | Retain Record.empty, including the child attribute |
| Empty array | Retain an empty Vector |
| `{ "child": { "name": null } }` | A present child containing Record.empty |
| NonEmptyVector of supported values | Encode as an ordinary array, recursively and in order |
| Untyped array decoding | Vector; do not infer NonEmptyVector from its length |
| Explicit NonEmptyVector typed read | Nonempty input succeeds; empty input returns a Consequence failure |
| Null element in a Record-owned sequence | Fail at conversion; never filter it out or shift indexes |
| XJson value, including Json.Null or arrays containing null | Preserve existing JSON semantics |

Null-attribute omission applies recursively to objects. It does not authorize
dropping a null array element, collapsing an empty child object, treating
empty String/false/zero as absent, or converting unknown objects to Strings.
Root arrays/scalars/null remain outside the XSemiStructuredData object contract.
Use XJson when retaining arbitrary JSON null values is the requirement.

For standalone Record value construction/replacement, a supplied null does
not leave a stored null attribute. For an explicit attribute replacement,
null removes that attribute. This is not a patch protocol: applying a patch
that omits a field must not erase an existing value. Existing distinct
merge/concatenation semantics must not be conflated with replacement.

The semantic roundtrip is relative to the normalized value, not the original
null-bearing input. Preserve retained property order, sequence order, exact
numeric meaning and nested empty structures. Normalization is idempotent.
Raw JSON continues to have its separate preservation contract.

## Work plan

1. **RCA-01 - Specify one complete route.** Update the affected working specs
   and author connected executable scenarios for nested Records containing
   null attributes, empty structures and NonEmptyVector values. Distinguish
   untyped decoding from explicit nonempty typed reads.
2. **RCA-02 - Implement the route in Core.** Connect existing Record codecs,
   XSemiStructuredData and ValueReader without requiring callers to sprinkle
   toVector or null filtering around their code. Share semantic conversion
   logic rather than round-tripping through JSON merely to check a Record.
3. **RCA-03 - Complete affected entry points.** Review Record constructors,
   data/create/createFull paths, Field-vector construction/copy, attribute
   replacement and supported JSON/YAML/HOCON conversion routes. Resolve
   inconsistent null-preserving paths and migrate their actual callers.
   Verify existing XML export handles NonEmptyVector as a collection where
   that export already supports sequences; do not add new format features.
4. **RCA-04 - Validate and review the integrated change.** Run focused Record,
   structured-data and typed-reader specifications together, including
   property-based normalization/order checks. Perform one independent review
   of the complete contract and its actual caller interactions. Run repository
   release validation before closure; do not repeat reviews per helper.
5. **RCA-05 - Deliver the consumer handoff.** Identify actual changed APIs,
   behavior, dependency revision and validation results for CNCF Phase 115.
   Preserve unrelated unfinished work and record remaining candidates.

## Implementation boundary

Likely targets are `org.goldenport.record` (including io),
`org.goldenport.schema.StructuredData*`, `XSemiStructuredData`, and the relevant
`org.goldenport.convert.ValueReader` paths and executable specifications.
Confirm the precise callable surface from code when implementation starts.

Use existing Consequence error handling and semantic boundary conversion.
Do not add repeated internal validation, input snapshots, hashes, TTL checks,
new collection registries, alternative Record models or a compatibility mode
that leaves two competing canonical null policies. This phase is not a
repository-wide List/Map replacement or a change to typed business models.
Do not expand it into unrelated placeholder methods or generic merge redesign.

## Completion

All required items in the checklist must have actual implementation and
behavioral evidence. Core closure requires a usable consumer handoff, not
CNCF Phase 115 completion. Dependency publication and downstream adoption
retain their own execution boundaries; this planning change performs neither.
