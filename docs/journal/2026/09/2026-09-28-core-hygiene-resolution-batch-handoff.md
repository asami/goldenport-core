# Hygiene Resolution Batch Handoff

Status: COMPLETE
Validated On: 2026-09-28
Validation Evidence: goldenport-core: invocation `core-hygiene-output-full-9aa7e76ae1584ae9`; 462 passed, 0 canceled, 0 ignored, 127 existing pending; receipt `6b809b9ef2293828b9d1af23eb0761c82efd205c30a461da6e7dc83dfce878ea`; lock released; goldenport-cncf: invocation `core-hygiene-output-ledger-full-01e8016228044729`; 3815 passed, 13 canceled, 1 ignored, 46 existing pending; receipt `8233e1dc3cba9efd396a784534b8704c6e38a4fc24b0f49be9bda229081495e7`; lock released; CLEAN focused review `25d3bcfafaf9da0952ad3438d3c4466f49ff4a8324961eab764370d7af1bd91c`
Created: 2026-09-28
Source Repository: /Users/asami/src/dev2026/goldenport-core
Target Repositories: /Users/asami/src/dev2026/goldenport-core; /Users/asami/src/dev2026/goldenport-cncf (one source ledger only)

## Included Hygiene

| ID | Source | Work Package | Required outcome |
| --- | --- | --- | --- |
| HYG-CORE-20260928-DEPRECATIONS | goldenport-core:docs/journal/2026/09/2026-09-28-core-compilation-output-hygiene.md | HP-001 | Remove 12 diagnosed deprecations with identical behavior |
| HYG-P63-CORE-SPEC-PRIVATE-TERM-NAMES | goldenport-cncf:docs/journal/2026/09/2026-09-17-phase-63-hygiene-follow-up.md | HP-001 | Rename private fixtures only |
| HYG-CORE-20260928-EXPECTED-XML-DIAGNOSTICS | goldenport-core:docs/journal/2026/09/2026-09-28-core-compilation-output-hygiene.md | HP-002 | Assert expected parser diagnostics without console noise |

Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-CORE-20260928-DEPRECATIONS
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-P63-CORE-SPEC-PRIVATE-TERM-NAMES
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

## Frozen boundary

- Core initially clean at `abeb4e6d79d7b55fad102dc8a62b7dc3b528cd16`.
- Core repair paths are exactly the four HP-001 targets and the one HP-002 target below. Management paths
  are this handoff and the core compilation/output diagnosis journal.
- CNCF allows only the original source ledger named above. Preserve every other
  CNCF path, its four existing local commits and the resolved CNCF-specific ID.
- Preserve public/protected signatures, failure contracts, naming behavior,
  dependencies, versions, production output and every test assertion.
- Do not alter ScalaTest reporting or Given/When/Then. No snapshot dump reproduced.
- Preserve existing pending cases and unrelated diagnostics; no feature work,
  public contract change, upstream workaround, publication, push or release.

## HP-001 — Mechanical deprecation and private fixture repair

- Hygiene IDs: HYG-CORE-20260928-DEPRECATIONS; HYG-P63-CORE-SPEC-PRIVATE-TERM-NAMES
- Repository: /Users/asami/src/dev2026/goldenport-core
- Targets:
  - `build.sbt`
  - `src/main/scala/org/goldenport/value/ContentAttributes.scala`
  - `src/main/scala/org/goldenport/record/RecordKeyNaming.scala`
  - `src/test/scala/org/goldenport/statemachine/TransitionDeciderSpec.scala`
- Allowed repair: replace the eleven deprecated two-argument calls with
  `Consequence.valueInvalid`; replace Char/String addition with interpolation;
  rename private fixture identifiers to `_start`, `_middle`, `_end`, `_machine`;
  maintain Scala header histories; add Compile `-deprecation` without suppressing
  warnings. No new executable spec is needed for identical constructor aliases;
  existing content-failure, record-name and transition tests cover the edits.
- Prohibited expansion: change API signatures, diagnostic taxonomy, parsing,
  assertions, test descriptions, pending scaffolds or output settings.
- Focused validation: `sbt --batch "testOnly org.goldenport.value.ContentAttributesSpec org.goldenport.record.RecordSpec org.goldenport.statemachine.TransitionDeciderSpec"`
- Dependencies: None.
- Package State: FOCUSED_PASS
- Evidence: invocation `core-hygiene-output-focused-0792ae6da5eb479d`; 23 passed, two pre-existing pending; zero warnings; receipt `80d50dbf423bea2bafd54c00305d751e9573e361c78b31f2312cd33407cb33e6`; lock released.

## HP-002 — Assert expected XML diagnostics without console noise

- Hygiene IDs: HYG-CORE-20260928-EXPECTED-XML-DIAGNOSTICS
- Repository: /Users/asami/src/dev2026/goldenport-core
- Targets: `src/test/scala/org/goldenport/configuration/source/file/FileConfigLoaderSpec.scala`
- Authority: explicit user clarification received before final review. This is
  an admitted amendment to the original boundary, not incidental new debt.
- Allowed repair: private test-only stderr capture around the malformed XML and
  forbidden DOCTYPE loader calls; forward other threads to the original stream,
  restore in finally, assert one fatal-error line and locale-independent markers.
- Prohibited expansion: production parser changes, XML security policy changes,
  broad output suppression, GWT capture, discarded failure assertions, global
  test serialization or reporter/logger changes.
- Focused validation: `sbt --batch "testOnly org.goldenport.configuration.source.file.FileConfigLoaderSpec"`
- Dependencies: HP-001.
- Package State: FOCUSED_PASS
- Evidence: invocation `core-hygiene-output-xml-focused-4f88157877e5456f`; five passed,
  zero warnings and zero raw stderr diagnostics; receipt
  `3a6a375bf5a8d0cd1a164e16dda033a20fa8a95aede4729912a060fdd03c3c5e`; lock released.

Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-CORE-20260928-EXPECTED-XML-DIAGNOSTICS
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

## Final focused review

One independent review covers all five targets as whole files, all three IDs and all
three management journals. Check headers, naming, behavior preservation,
compatibility and focused evidence. Existing public/protected naming debt stays
outside this repair. Any current blocker stops the batch without commit.

## Final full-validation gate

1. Core: `sbt --batch clean test` — require 462 passed, the same 127 pending,
   zero compiler warnings and zero unwanted snapshot dumps or expected XML fatal-error console lines.
2. CNCF: `sbt --batch test` — validate the source-ledger repository once, after
   core. No CNCF executable input is changed or dependency artifact refreshed.

Run once per repository after CLEAN review through the registered serialized
SBT runner. A failed gate stops the batch without commit.

## Completion contract

Predeclared post-gate fields only: source Hygiene Status RESOLVED, Resolution
Batch, Validated On date and exact validation invocation/receipt references;
batch Status COMPLETE and the same validation/review references. No semantic
documentation or program change is permitted after the accepted gate.

Grouped local acceptance commits contain the repair and mechanical closure.
Commit core before CNCF's original source record. The states become authoritative
only after both commits succeed; commit identities are reported externally.
