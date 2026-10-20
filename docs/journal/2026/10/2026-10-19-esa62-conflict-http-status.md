# ESA62 Conflict HTTP Status Repair

## Context and authority

The parent workflow recorded `VAL-ESA62-CONFLICT-HTTP-001` for
PHASE-6.2 / ESA62-02+03. Its server validation recorded 53 passing scenarios out
of 55: changed-payload and changed-context replay conflicts returned HTTP 500
where the existing consumer contract requires 409.

The frozen repair is limited to the shared library's generated status mapping,
its executable specification, the canonical Conclusion specification, and this
journal. The existing `State/Conflict` taxonomy reached the State category's
500 fallback because `Conclusion.Status.webCodeOf` omitted the Conflict symptom.
The existing public operation conflict helper likewise reached the Operation
category's 400 fallback. `WebCode.Conflict` already represents 409.

## Bounded correction

Conflict now maps to `WebCode.Conflict` before category fallback for every
taxonomy category. Status continues to be generated during Conclusion
materialization. The existing non-Conflict symptom mappings, category
fallbacks, and Success interpretation's HTTP 200 mapping are preserved.

The canonical Status and WebCode section records this mapping.
`ConclusionHttpStatusSpec` specifies all taxonomy categories in an exhaustive
table and uses ScalaCheck to vary category and diagnostic message during
materialization. It also specifies both public conflict helpers and preserved
500, 400, 401, 403, 404, and 200 behavior. Each scenario places Given, When,
and Then beside setup, interpretation, and expectations.

## Validation ownership

The editing worker has not executed validation. The parent owns the focused
status and existing Consequence/Conclusion policy, presenter, source-position,
and throwable specifications, the normal local artifact refresh, and the
original 55-scenario server validation with runtime preparation. The full
library suite remains pending at the release gate. This entry records no new
execution result or acceptance claim.

## HTTP carrier correction: VAL-ESA62-CONFLICT-HTTP-002

The parent recorded the remaining PHASE-6.2 / ESA62-02+03 failure after the
central Conclusion correction and normal local artifact refresh. The actual
consumer still converted both replay conflicts to 500 because its HTTP bridge
uses `HttpStatus.fromInt(conclusion.status.webCode.code)` with a 500 fallback,
and the protocol carrier did not recognize 409.

The frozen carrier correction adds `HttpStatus.Conflict` and its explicit 409
integer mapping. It preserves all established named mappings, the dynamic 3xx
mapping, and the unsupported-code parser fallback. The earlier Conclusion
correction and its executable specification remain in place.

`HttpStatusConflictSpec` describes direct 409 singleton selection, conversion
of public `Consequence.stateConflict` through the normal status bridge into a
text response, and parser preservation of conflict status, UTF-8 body, and
headers. ScalaCheck varies the parser body. Separate scenarios preserve every
existing named status including 204, plus unsupported selections and the 299
parser's 500 fallback. Every scenario places Given, When, and Then adjacent to
setup, interpretation, and expectations. The canonical Status and WebCode
section links this carrier specification.

The worker has not executed these new specifications or validation. The parent
owns the selected focused specifications, normal local artifact refresh,
original 55-scenario consumer validation, and runtime preparation. Full
validation remains pending at the release gate; this entry claims no new
passing result or acceptance.
