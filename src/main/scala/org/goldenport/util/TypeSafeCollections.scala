package org.goldenport.util

import cats.Eq
import org.goldenport.id.{Identifiable, Identified, UniversalId}

/** Equality and identity operations for immutable Vector.
  *
  * `typeSafe` captures the receiver element type before any comparison argument
  * is supplied. Select one identity relation explicitly with `identifiedBy` or
  * `identifiableBy`; this captures its ID type before any lookup argument.
  * No adapters are implicitly selected when both relations exist.
  */
object TypeSafeCollections {
  extension [A](values: Vector[A])
    def typeSafe: VectorValues[A] = new VectorValues(values)

  final class VectorValues[A] private[TypeSafeCollections] (values: Vector[A]) {
    /** Linear scan using the supplied Eq; does not use universal equality. */
    def containsEq[B](value: B)(using equality: Eq[A])(using same: B =:= A): Boolean =
      values.exists(element => equality.eqv(element, same(value)))

    def identifiedBy[I <: UniversalId](relation: Identified[A, I])(using equality: Eq[I]): VectorIds[A, I] =
      new VectorIds(values, element => Some(relation.id(element)), equality)

    def identifiableBy[I <: UniversalId](relation: Identifiable[A, I])(using equality: Eq[I]): VectorIds[A, I] =
      new VectorIds(values, element => relation.id(element), equality)
  }

  /** ID lookup uses the first matching present ID. Distinct preserves order,
    * keeping the first entity for each Eq-equivalent present ID and every
    * unidentified occurrence, including repeated references. Payload conflicts
    * are not resolved. Identity is separate from value and reference equality.
    *
    * Lookup is O(n); distinct is O(n²) in the worst case. Both are linear Eq
    * scans and use neither Hash nor Ordering.
    */
  final class VectorIds[A, I <: UniversalId] private[TypeSafeCollections] (
    values: Vector[A],
    identity: A => Option[I],
    equality: Eq[I]
  ) {
    def containsId(id: I): Boolean = findById(id).isDefined

    def findById(id: I): Option[A] =
      values.find(element => identity(element).exists(found => equality.eqv(found, id)))

    def distinctById: Vector[A] = {
      val (result, _) = values.foldLeft((Vector.empty[A], Vector.empty[I])) {
        case ((kept, seen), element) =>
          identity(element) match {
            case None => (kept :+ element, seen)
            case Some(id) if seen.exists(previous => equality.eqv(previous, id)) => (kept, seen)
            case Some(id) => (kept :+ element, seen :+ id)
          }
      }
      result
    }
  }
}
