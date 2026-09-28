# Core compilation and output hygiene diagnosis

Date: 2026-09-28

The user requested goldenport-core Hygiene, compiler warning and unwanted
snapshot-output closure. The initial repository was clean at
`abeb4e6d79d7b55fad102dc8a62b7dc3b528cd16`.

## HYG-CORE-20260928-DEPRECATIONS

Hygiene Status: RESOLVED
Resolution Batch: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Validated On: 2026-09-28
Validation Evidence: goldenport-core: invocation `core-hygiene-output-full-9aa7e76ae1584ae9`; 462 passed, 0 canceled, 0 ignored, 127 existing pending; receipt `6b809b9ef2293828b9d1af23eb0761c82efd205c30a461da6e7dc83dfce878ea`; lock released; goldenport-cncf: invocation `core-hygiene-output-ledger-full-01e8016228044729`; 3815 passed, 13 canceled, 1 ignored, 46 existing pending; receipt `8233e1dc3cba9efd396a784534b8704c6e38a4fc24b0f49be9bda229081495e7`; lock released; CLEAN focused review `25d3bcfafaf9da0952ad3438d3c4466f49ff4a8324961eab764370d7af1bd91c`
Repository: goldenport-core
Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-CORE-20260928-DEPRECATIONS
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

`sbt --batch clean test` passed 462 tests in 149 suites, with 127 existing pending
scenarios. Invocation `core-hygiene-output-diagnostic-e9fcd5a7f4dd47bb`, receipt
`f0b2fe0800894e27b14e3b34398b459b6064ac7fd6a38f8b0475a852742ff746`.
An additional clean Compile with temporary `-deprecation` located 12 warnings:
11 calls to deprecated `Consequence.failValueInvalid(value, datatype)` in
`ContentAttributes.scala` and one deprecated Char/String concatenation in
`RecordKeyNaming.scala`. Invocation
`core-hygiene-output-deprecations-3283ad52b3be4f76`, receipt
`3780e6a1a2c333ffedc911b0be31aa02f74e7274720d5029cd2d9881724aea09`.

Both two-argument failure constructors have the identical implementation,
`Consequence.Failure(Conclusion.valueInvalid(value, datatype))`. Replacing the
deprecated entry point preserves status, taxonomy, message and metadata. String
interpolation preserves the uppercase Char and remaining String. Enable explicit
deprecation diagnostics so future occurrences remain visible.

## Existing fixture naming record

Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-P63-CORE-SPEC-PRIVATE-TERM-NAMES
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

HYG-P63-CORE-SPEC-PRIVATE-TERM-NAMES is owned by core and originates in
`goldenport-cncf:docs/journal/2026/09/2026-09-17-phase-63-hygiene-follow-up.md`.
Only the private start/middle/end/machine fixture identifiers in
`TransitionDeciderSpec.scala` are admitted. Test behavior and descriptions remain.

## Snapshot and other output

The baseline contained no unwanted snapshot dump. All 17 snapshot matches were
test descriptions; Given/When/Then and ScalaTest reporting are preserved. No
reporter option, output capture or production logger change is required.

Separate existing observations are the 127 pending scaffolds and POI's missing
Log4j2 implementation diagnostic. These remain outside this bounded repair.
The expected XML diagnostics were initially excluded as a separate output kind;
the user's subsequent clarification explicitly admits them below. Public/protected naming predating this repair remains
separate debt because public named arguments and extension signatures must not
change during a behavior-preserving warning repair.

## HYG-CORE-20260928-EXPECTED-XML-DIAGNOSTICS

Hygiene Status: RESOLVED
Resolution Batch: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Validated On: 2026-09-28
Validation Evidence: goldenport-core: invocation `core-hygiene-output-full-9aa7e76ae1584ae9`; 462 passed, 0 canceled, 0 ignored, 127 existing pending; receipt `6b809b9ef2293828b9d1af23eb0761c82efd205c30a461da6e7dc83dfce878ea`; lock released; goldenport-cncf: invocation `core-hygiene-output-ledger-full-01e8016228044729`; 3815 passed, 13 canceled, 1 ignored, 46 existing pending; receipt `8233e1dc3cba9efd396a784534b8704c6e38a4fc24b0f49be9bda229081495e7`; lock released; CLEAN focused review `25d3bcfafaf9da0952ad3438d3c4466f49ff4a8324961eab764370d7af1bd91c`
Repository: goldenport-core
Hygiene Triage: HANDED_OFF
Hygiene ID: HYG-CORE-20260928-EXPECTED-XML-DIAGNOSTICS
Handoff Journal: goldenport-core:docs/journal/2026/09/2026-09-28-core-hygiene-resolution-batch-handoff.md
Handed Off On: 2026-09-28

The user clarified in a side conversation that expected parser diagnostics also
belong to this Hygiene task. The initial full run printed two SAX fatal-error
lines from FileConfigLoaderSpec's malformed XML and forbidden DOCTYPE cases.
ConfigTextDecoder has no test-injection seam; its default JAXP handler writes
directly to System.err. Preserve the production parser, hardening and Consequence
failure contract. Capture only those synchronous calls in the spec, forwarding
other threads' stderr to the original stream and restoring it in finally. Assert
the captured fatal diagnostic as additional evidence without depending on the
machine's localized prose. GWT clauses and assertions execute outside capture.
Unexpected extra captured output fails the diagnostic assertion.
