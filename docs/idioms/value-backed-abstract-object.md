# Value-Backed Abstract Object

**Idiom:** ValueBackedAbstractObject

---

## Intent

Provide a reusable structural idiom for objects whose **conceptual identity**
is an abstract object, while their **operational content** is carried as a
single immutable value object.

This idiom separates:

- **Concept** (abstract class)
- **Value representation** (case class)
- **Default concrete realization** (case class)

and allows the same structure to be reused with different semantic names
(e.g. Value, Core, Params, Payload, Snapshot, Spec).

---

## Structure

- Abstract class representing the concept
- A single value object carrying all parameters
- A Holder exposing the value’s fields as stable accessors
- A default Instance used by most clients
- A factory method hiding the concrete class

---

## Canonical Form

```scala
abstract class ValueBackedObject[V] {
  def value: V
}

object ValueBackedObject {

  trait Holder[V] {
    def value: V
  }
}
```

A concrete usage typically follows this shape:

```scala
abstract class X extends X.Value.Holder {
  def value: X.Value
}

object X {

  final case class Value(
    // parameters
  )

  object Value {

    trait Holder {
      def value: Value

      // accessor methods
    }
  }

  final case class Instance(
    value: Value
  ) extends X

  def apply(
    // parameters
  ): X =
    Instance(
      Value(
        // parameters
      )
    )
}
```

---

## Variants

- **Trait-based variant**  
  Used when `X` represents a capability or mixin rather than a conceptual root.

- **Multiple instance variants**  
  Useful when validated / enriched / traced instances are required.

---

## When to Use

- Protocol-level request objects
- Snapshot / context objects
- Specification or descriptor objects
- Objects whose API must remain stable while their internal representation evolves

---

## When NOT to Use

- Pure value objects (simple case classes)
- Runtime execution objects (jobs, commands, handlers)
- Objects requiring heavy inheritance hierarchies

---

## Known Uses

- `OperationRequest` (ProtocolRequest idiom)
- `ExecutionContextSnapshot`
- `SessionContext`

---

## Notes

- The name of the value object (`Value` / `Core` / `Params` / `Payload` / etc.)
  is **semantic**, not structural.
- This idiom is intended to be referenced **by name** in design discussions,
  reviews, and AI-assisted code generation.
