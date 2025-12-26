# Holder Idiom

**Idiom:** Holder

---

## Intent

Provide a stable accessor facade over a value object while keeping the
underlying representation encapsulated.

This idiom emphasizes:
- stable public accessors
- representation hiding
- clear separation between concept and data

---

## Canonical Example

```scala
abstract class X extends X.Core.Holder {
  def core: X.Core
}

object X {

  final case class Core(
    name: String,
    enabled: Boolean
  )

  object Core {

    trait Holder {
      def core: Core

      def name: String = core.name
      def enabled: Boolean = core.enabled
    }
  }
}
```

---

## When to Use

- Protocol or schema definitions
- Long-lived APIs that may change internal representation
- Domain objects that need stable accessor semantics
