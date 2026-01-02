Collection Idiom Rules
======================

This document defines the Collection vs Group distinction and the
canonical Collection idiom for core.

----------------------------------------------------------------------
1. Collection vs Group
----------------------------------------------------------------------

Collection:
- plain aggregation
- empty is natural and allowed
- no semantic invariant beyond element type

Group:
- semantic aggregation
- may be non-empty or invariant-bearing
- can enforce additional meaning

----------------------------------------------------------------------
2. Collection idiom
----------------------------------------------------------------------

For Collection types, apply this pattern:

final case class XCollection(
  xs: Vector[X] = Vector.empty
)

object XCollection {
  val empty: XCollection = new XCollection(Vector.empty)
  def apply(): XCollection = empty
}

Notes:
- default argument preserves source convenience
- named empty improves clarity
- zero-arg apply preserves binary compatibility

----------------------------------------------------------------------
3. Binary compatibility rationale
----------------------------------------------------------------------

Default arguments alone are insufficient for binary compatibility.
Named empty and zero-arg apply provide stable call sites across
compiled binaries and improve Java/DSL friendliness.

----------------------------------------------------------------------
4. Scope in core
----------------------------------------------------------------------

Apply the idiom to:
- technical aggregation types
- protocol/handler/engine collections

Exclude:
- Group types
- NonEmpty collections
- types that must forbid empty by semantics
