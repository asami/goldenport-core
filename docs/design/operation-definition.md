# OperationDefinition Design

This document explains the intent and rationale behind the project rules for
OperationDefinition and semantic request interpretation.

## Design Intent

OperationDefinition is the semantic boundary between syntactic input and
operation execution. The goal is to keep CLI and resolver layers syntax-only
while ensuring that parameter meaning and typing are centralized.

## Parameter-First Interpretation

ParameterDefinition acts as the single source of truth for parameter semantics.
This allows interpretation logic to stay consistent and testable, even when
requests originate from different front-end protocols.

## ParameterDefinition Requirement

Every parameter accessed by `take_*` utilities is declared in
`RequestDefinition.parameters`.

When a parameter is not defined in ParameterDefinition, `take_*` fails and
`createOperationRequest` returns `Consequence.Failure`. This behavior is
verified by specification tests and makes ParameterDefinition the single
source of parameter meaning.

`take_*` utilities do not read raw Request fields directly. They resolve
through ParameterDefinition so that required/optional semantics and typing
are centralized in one place.

## Applicative Construction

Applicative construction keeps validation order-independent and enables error
aggregation. This style makes it clear that multiple parameters are interpreted
in parallel, and the final OperationRequest is assembled only when all parts
are available.

Example (standard form, zip3With):

```scala
final case class CreateUser(
  id: String,
  name: String,
  birthday: Option[ZonedDateTime]
) extends OperationRequest

def createOperationRequest(
  req: Request
): Consequence[OperationRequest] =
  using val reqctx: Request = req

  take_string("id")
    .zip3With(
      take_string("name"),
      get_datetime("birthday")
    )(CreateUser(_, _, _))
```

This example uses the native `zip3With` applicative form.

cats-based `mapN` / `|@|` are optional conveniences when cats is already
available; the standard form does not depend on cats.

This example illustrates the **recommended standard form**:
- `Request` is accepted explicitly as the semantic boundary input.
- A semantic interpretation context is established using `using val reqctx = req`.
- All parameter extraction goes through `take_*` utilities.
- The result is a domain-level `OperationRequest`, not a generic container.

## Request Context

Using an implicit Request keeps extraction helpers concise while preserving
explicit ownership of interpretation inside OperationDefinition. This supports
readability without pushing semantic rules outward to the CLI.

## Application-Level Usage Pattern

This section shows the intended *application-facing* usage pattern, where
OperationRequest types own their semantic definition via an associated
OperationDefinition.

Example:

```scala
case class Query(
  query: String
) extends OperationRequest

object Query {
  val operation: OperationDefinition =
    new OperationDefinition {
      override val specification =
        OperationDefinition.Specification(
          name = "query",
          request = RequestDefinition(
            parameters = List(
              ParameterDefinition(
                name = "query",
                kind = ParameterDefinition.Kind.Argument,
                domain = ValueDomain(
                  datatype = XString,
                  multiplicity = Multiplicity.One
                )
              )
            )
          ),
          response = ResponseDefinition(Nil)
        )

      override def createOperationRequest(
        req: Request
      ): Consequence[OperationRequest] =
        using val reqctx: Request = req

        take_string("query").map(Query(_))
    }
}
```

The application registers operations via ServiceDefinition:

```scala
val service = ServiceDefinition(
  name = "search",
  operations = OperationDefinitionGroup(
    NonEmptyVector.of(Query.operation)
  )
)

cli.register(service)
```

CLI invocation remains purely syntactic:

```bash
sie search query "hello world"
```

Key properties of this pattern:
- Application code depends only on OperationRequest and OperationDefinition.
- CLI code remains string-based and syntax-only.
- Parameter semantics and typing are fully owned by OperationDefinition.
- DataType and multiplicity flow through resolveParameter but do not leak into the CLI layer.
