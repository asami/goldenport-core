# HTTP Service Unavailable status

Date: 2026-09-29
Finding: CORE-HTTP503-001

## Scope and cause

The core HTTP status vocabulary did not represent status 503. `HttpStatus.fromInt`
therefore returned no status for 503, and the established parser fallback produced
500. The status vocabulary is owned by goldenport-core, so this transport mapping
belongs here rather than in a downstream adapter.

## Additive repair

`HttpStatus.ServiceUnavailable` now represents 503, and `fromInt(503)` selects
that status. Existing status mappings and the fallback for unsupported codes remain
unchanged. The executable specification records generated public-response and
public-parser properties for UTF-8 bodies, content types, and case-insensitive
headers, including the existing unsupported-code fallback.

## Pending handoff

Validation, independent review, and commit remain pending until their actual
evidence is recorded. After the core change is accepted and committed, the unchanged
`goldenport-core_3:0.4.3-SNAPSHOT` coordinate requires a local refresh before
PHASE-69.6 resumes its downstream HTTP acceptance work.
