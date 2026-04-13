# Structured Consequence Utility Migration

Date: Apr. 14, 2026

## Context

The recent error-model work moved framework failures away from message-only
or alias-style helpers toward semantic utility methods whose names describe
the failure meaning.

The implementation direction is:

- `Consequence.failure(...)` is a last-resort message-oriented fallback.
- `Consequence.fail(...)` remains a low-level structured builder for cases
  where the caller intentionally supplies taxonomy, cause, and facets.
- `Consequence.failXxx(...)` and `Conclusion.failXxx(...)` are legacy aliases
  and should not be used by new code.
- Recurring failures should gain semantic utilities such as
  `argumentMissingInput(...)`.

## Work Done

`Conclusion` was extended with semantic structured utilities for the missing
input cases needed by protocol and projection code.

Existing `failXxx(...)` aliases were marked as deprecated so new call sites
prefer semantic utility names.  The low-level `fail(...)` builder was kept as
the explicit escape hatch for application-specific structured failures.

Executable specifications were added around the missing-input paths, including
Args ingress and Projection/Egress collection behavior.

## Downstream Check

CNCF was checked after publishing simplemodeling-lib locally.  Its test
compilation succeeded and no runtime call sites using legacy
`Consequence.failXxx(...)` or `Conclusion.failXxx(...)` aliases remained.

## Remaining Direction

New framework failures should first look for an existing semantic utility.
If the failure is repeated and no utility exists, add one at the semantic
layer instead of spreading low-level taxonomy/facet construction across
call sites.
