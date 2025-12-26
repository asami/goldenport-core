# Value-Backed Idiom

**Idiom:** ValueBacked

---

## Intent

Aggregate all relevant state into a single case class so that data operations
are explicit, testable, and easy to compose.

This idiom emphasizes:
- case class aggregation
- copy / transform workflows
- applicative validation
- clear data flow in tests and logs

---

## Canonical Example

```scala
final case class UserInput(
  name: String,
  email: String
)

object UserInput {
  def normalize(input: UserInput): UserInput =
    input.copy(
      name = input.name.trim,
      email = input.email.trim.toLowerCase
    )
}
```

---

## When to Use

- Validation pipelines
- Normalization / transformation steps
- Data capture and transport objects
- When you need stable, inspectable intermediate values
