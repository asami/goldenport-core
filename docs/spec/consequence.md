# Consequence

## Role

`Consequence` represents success or failure as values.
Failures are carried by `Conclusion`.

## Conclusion.message

`Conclusion.message` is derived from the observation:

- `Conclusion.message` = `Observation.displayMessage`

This keeps the message deterministic and consistent with core observation data.

## Exception and Escalation Use

The derived message is used for system-facing summaries in:

- `ConsequenceException`
- `RAISE` / `RAISEC` escalation helpers

These uses are non-UI and rely on the same deterministic message source.


## Working Specification (Executable Spec)

This specification is complemented by an executable *working specification*
implemented as ScalaTest tests.

- Markdown specifications in this document describe **design intent and semantics**.
- ScalaTest specifications serve as **executable, verifiable semantics** that
  MUST remain consistent with this document.

See the following test as the authoritative working specification for
validation-style usage of `Consequence`:

- `ConsequenceValidationSpec` (ScalaTest, AnyWordSpec)

When updating behavior related to:
- `Consequence.zip` / `zipN`
- `Conclusion.++` aggregation
- validation-style composition

both this document **and** the corresponding working specification
MUST be updated together.

## Conclusion Aggregation (`++`)

`Conclusion.++` combines two `Conclusion` instances by aggregating
their semantic failure information.

### Observation Order

- Observations are concatenated in order.
- The left-hand side observations come first, followed by the right-hand side.

```
(c1 ++ c2).observations
  = c1.observations ++ c2.observations
```

- No reordering, deduplication, or grouping is performed.
- The order is deterministic and significant.

### Severity Composition

- The resulting severity is the maximum (most severe) of the two.

```
(c1 ++ c2).severity
  = max(c1.severity, c2.severity)
```

This ensures that the aggregated conclusion represents the most critical failure.

### Cause Composition

- If both conclusions have the same cause, that cause is preserved.
- If the causes differ, the resulting cause is set to `Composite`.

This reflects that multiple independent failures contributed to the result.

### Properties

- `Conclusion.++` is associative.
- `Conclusion.++` is not commutative.
- No semantic information is lost during aggregation.

`Conclusion.++` is primarily used by applicative-style composition
(`Consequence.zip`, `zipN`) to collect and represent multiple failures.
