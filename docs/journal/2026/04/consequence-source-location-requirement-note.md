# Consequence Source Location Requirement

Date: 2026-04-08

## Requirement

`Consequence` must be able to carry the source location of an error cause in a stable, observable form.

Minimum required location fields:
- source file
- line number
- optional column number

The location must be attached to the resulting `Cause` / `Conclusion` so it can be used by:
- logs
- observability output
- error rendering
- diagnostics in tests

## Non-requirement

The implementation mechanism is not constrained.

Allowed implementation approaches include:
- Scala 3 inline + macro
- plain method + explicit `SourcePosition`
- helper wrapper that captures source location indirectly
- any other implementation that preserves the required behavior

The implementation choice should favor:
- compiler stability
- maintainability
- observable correctness

over macro cleverness.

## Acceptance Criteria

The following behaviors should be testable:
- `failRecordNotFound(...)` attaches file/line information to the resulting cause
- `successOrPropertyNotFound(...)` attaches file/line information to the resulting cause
- `successOrRecordNotFound(...)` attaches file/line information to the resulting cause

Tests should verify behavior, not implementation technique.

## Design Guidance

If macro- or inline-heavy implementations cause compiler instability, the implementation should be simplified.
The requirement is source-location fidelity in the emitted `Cause`, not preservation of a specific macro style.
