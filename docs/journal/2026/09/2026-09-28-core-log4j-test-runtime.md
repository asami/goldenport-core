# Core Log4j test runtime follow-up

Date: 2026-09-28
Incident ID: CORE-LOG4J-20260928
Hygiene ID: HYG-CORE-20260928-LOG4J-TEST-PROVIDER
Hygiene Status: RESOLVED
Repository: goldenport-core
Validated On: 2026-09-28

The user requested addressing the remaining Log4j2 diagnostic recorded in
[the compilation and output hygiene journal](2026-09-28-core-compilation-output-hygiene.md).
This is a separate bounded follow-up under `cncf-diagnose-fix`. The initial
main HEAD was `f851886357ea8186c67471a4de14b6ab249c93d1`, with a clean worktree.

## Root cause confirmed

The previous full clean test passed but emitted one missing-implementation
StatusLogger diagnostic. POI 5.2.5 transitively brings `log4j-api` 2.21.1;
core's Test dependency classpath contained no Log4j provider. Inspection of the
cached API's LogManager bytecode confirms that an empty provider map emits
the observed message before selecting the SimpleLogger fallback. This is
test runtime configuration, rather than a failed test or compiler warning.

The prior full-test receipt was
`6b809b9ef2293828b9d1af23eb0761c82efd205c30a461da6e7dc83dfce878ea`.
Its recorded log contained the diagnostic at line 277. No duplicate baseline
run was necessary. RecordDecoderSpec's existing workbook import/export and
roundtrip cases exercise the initialization path.

## Repair and ownership

`build.sbt` adds `org.apache.logging.log4j:log4j-to-jul:2.21.1` in Test scope,
matching POI's resolved Log4j API version. This provides a logging implementation
that forwards Log4j calls to JDK `java.util.logging`. Real diagnostics remain
available, and consuming applications retain logging-backend ownership.
Apache's [installation guidance](https://logging.apache.org/log4j/2.x/manual/installation.html)
places a library's test logging implementation in test scope; the
[bridge documentation](https://logging.apache.org/log4j/2.x/log4j-to-jul.html)
describes forwarding to JUL.

No production source, logger adapter, test assertion, reporter option, stream
capture, logging level, dependency version upgrade or release coordinate was
changed. Given/When/Then reporting is preserved. Existing workbook tests cover
this repair; no duplicate dependency-shape test was added. The previous journal
continues to describe its historical residual accurately.

## Validation

One serialized invocation ran:

```text
sbt --batch "show Compile / dependencyClasspath" "show Test / dependencyClasspath" clean test
```

Java 25.0.4, SBT 1.9.8 and Scala 3.3.8 were unchanged. The registered
`cncf_command_runner` used normal dependency caches and the shared lock.
The full suite is appropriate because provider selection affects the entire
test runtime and full-test output was the original observation. It also runs
the nearest existing workbook regression, so no redundant focused run is needed.

- Main: 178 Scala sources; Test: 151 Scala sources recompiled.
- 462 tests succeeded in 149 completed suites; zero failures, canceled or
  ignored tests. The 127 existing pending scenarios are unchanged.
- Missing-provider StatusLogger diagnostic: zero, previously one.
- Compiler warnings and unexpected raw output: zero.
- Given/When/Then/And information lines: 1948, unchanged.
- All 17 snapshot matches are unchanged spec descriptions or GWT text;
  literal snapshot dumps: zero.
- Compile dependency classpath contains only `log4j-api` 2.21.1 among Log4j
  modules. Test contains that API and `log4j-to-jul` 2.21.1. The added backend
  is absent from Compile.
- `git diff --check` passed. The journal was added after verifying the
  executable tree and needs no additional runtime validation.

Invocation: `core-log4j-output-full-ead251ae76c14f40`.
Immutable receipt SHA-256:
`2a3982f9efd0feecae079196238012a594a710093c533b3733a98fa2e1e3777a`.
Wrapper and SBT exit codes: 0; terminal marker: `lock=released`.

Current Incident Blocker: CLOSED. No repair remains. Broader downstream
validation is unnecessary for a Test-only dependency. Separate Hygiene ledger:
the pre-existing pending scaffolds and public/protected naming debt retain
their previous status; no additional item was admitted. Development Candidate
ledger: none. Current Infrastructure Issue: none.

At the diagnosis workflow boundary, the validated `build.sbt` repair and this
journal were uncommitted. The user subsequently requested a local acceptance
commit. There was no publication or change to another repository.

## Agent Usage Summary

The bounded edit used GPT-5.6 Luna high; serialized validation used GPT-5.6 Luna
medium. The parent profile was unavailable and is recorded as unknown. All
delegation used the registered custom roles with healthy resolution.

| Agent identity / type | Model | Reasoning effort | Roles / states | START | RESUME | DIRECT | Scope | Outcome |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| /root / parent-task | unknown | unknown | diagnosis, verification, bookkeeping / DIAGNOSE, VERIFY | 0 | 0 | 1 | Causal evidence, frozen manifest, diff and output audit, journal | Incident closed |
| /root/core_log4j_fix / cncf_fix_worker_luna | gpt-5.6-luna | high | fix / FIX | 1 | 0 | 0 | Test-only bridge and comment in build.sbt | Manifest satisfied |
| /root/sbt_receipt_2bedd0883b3f77bb093e / cncf_command_runner | gpt-5.6-luna | medium | sbt / SBT | 1 | 0 | 0 | Compile/Test classpaths and clean test | Success; receipt verified; lock released |

Parent Direct Repair Gate was rejected because the change touches dependency
coordinates and provider classloading. The registered fix worker owns the edit.
Parent documentation and evidence capture are bookkeeping under the standalone
workflow. Disposable incident, manifest, receipts, output and scope audits are
retained under `/tmp/skill.cncf.d/c-5270def9679cc29494e818ca28cf46b7`.
