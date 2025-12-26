# Value-Backed Abstract Object

**Idiom:** ValueBackedAbstractObject

---

## Intent

Provide a reusable structural idiom for objects whose **conceptual identity**
is an abstract object, while their **operational content** is carried as a
single immutable value object.

The primary motivation of this idiom is to encapsulate data in a case class to maximize operability (copy, transform, validation), not to satisfy inheritance or identity concerns.

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

## Core Motivation

The decisive benefit of value-backed design is keeping all mutable concerns out of the abstract object and concentrating data manipulation on an immutable case class. This ensures that the abstract concept remains stable and focused on behavior, while the value object handles all data operations such as copying, transformation, and validation.

---

## Design Rationale (Background)

- ValueBacked centers on case class operability so that copy, transform, and validation remain simple and testable.
- The abstract class must not own data; it exists to represent meaning and provide a stable API facade.
- Treating a value object as a passive container (never transformed) is an anti-pattern that defeats the idiom's purpose.

---

## Related Idioms

- Value-Backed Idiom
- Holder Idiom

---

## Constituent Idioms

### Value-Backed Idiom

This idiom involves aggregating all state into a single case class. This practice emphasizes operability, enabling easy copying, diffing, applicative validation, testing, and logging of the data. The value object acts as the canonical representation of the state.

### Holder Idiom

The `Holder` pattern provides a stable accessor facade over a value object. It exposes the value’s fields through well-defined accessors, ensuring API stability and representation hiding. This facade allows clients to interact with the data without depending on the concrete value object’s structure.

### Abstract Concept Idiom

The abstract class or trait represents *conceptual meaning*, not data storage. It should not own any fields directly. Instead, it defines the conceptual interface or identity that the value-backed object embodies, separating concerns of identity and data representation.

### Default Instance Idiom

A default concrete case class (`Instance`) serves as the standard realization of the abstract concept. It simply holds the value object, providing a straightforward implementation that most clients use without needing custom subclasses.

---

## Idiom Composition

The *Value-Backed Abstract Object* idiom is a composition of the above idioms: Value-Backed, Holder, Abstract Concept, and Default Instance. Each idiom can be reused independently, allowing flexible design choices depending on the needs of the system.

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
- When the value object itself is the primary subject of transformation

---

## When NOT to Use

- Pure value objects (simple case classes)
- Runtime execution objects (jobs, commands, handlers)
- Objects requiring heavy inheritance hierarchies
- When the value is rarely or never manipulated after construction

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
- Misusing ValueBackedAbstractClass without actively using the value object is an anti-pattern.
