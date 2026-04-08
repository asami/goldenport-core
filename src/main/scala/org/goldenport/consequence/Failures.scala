package org.goldenport.consequence

import org.goldenport.Consequence
import org.goldenport.Conclusion
import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Taxonomy
import org.goldenport.provisional.observation.Cause
import org.goldenport.observation.Descriptor
import org.goldenport.observation.SourcePosition
import org.goldenport.record.Record

/*
 * @since   Jan. 31, 2026
 *  version Feb. 28, 2026
 *  version Mar. 10, 2026
 * @version Apr.  8, 2026
 * @author  ASAMI, Tomoharu
 */
object Failures {
  inline def fail(o: Observation): Consequence.Failure[Nothing] =
    fail(o, SourcePositionMacro.position())

  def fail(o: Observation, pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failure(pos, o))

  inline def fail(taxonomy: Taxonomy, cause: Cause): Consequence.Failure[Nothing] =
    fail(taxonomy, cause, SourcePositionMacro.position())

  def fail(
    taxonomy: Taxonomy,
    cause: Cause,
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failure(pos, taxonomy, cause))

  inline def fail(
    taxonomy: Taxonomy,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    fail(taxonomy, facets, SourcePositionMacro.position())

  def fail(
    taxonomy: Taxonomy,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failure(pos, taxonomy, facets))

  inline def fail(
    taxonomy: Taxonomy
  ): Consequence.Failure[Nothing] =
    fail(taxonomy, Nil, SourcePositionMacro.position())

  inline def fail(c: Conclusion): Consequence.Failure[Nothing] =
    fail(c, SourcePositionMacro.position())

  def fail(c: Conclusion, pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(c.withSourcePosition(pos))

  inline def resourceInconsistency: Consequence.Failure[Nothing] =
    resourceInconsistency(SourcePositionMacro.position())

  def resourceInconsistency(pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failResourceInconsistency(pos))

  inline def recordNotFound(key: String, rec: Record): Consequence.Failure[Nothing] =
    recordNotFound(key, rec, SourcePositionMacro.position())

  def recordNotFound(
    key: String,
    rec: Record,
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failRecordNotFound(pos, key, rec))

  inline def operationNotFound(name: String): Consequence.Failure[Nothing] =
    operationNotFound(name, SourcePositionMacro.position())

  def operationNotFound(name: String, pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failOperationNotFound(pos, name))

  inline def unreachableReached: Consequence.Failure[Nothing] =
    unreachableReached(SourcePositionMacro.position())

  def unreachableReached(pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failUnreachableReached(pos))

  inline def uninitializedState: Consequence.Failure[Nothing] =
    uninitializedState(SourcePositionMacro.position())

  def uninitializedState(pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failUninitializedState(pos))

  inline def uninitializedState(c: Conclusion): Consequence.Failure[Nothing] =
    uninitializedState(c, SourcePositionMacro.position())

  def uninitializedState(c: Conclusion, pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failUninitializedState(pos) ++ c)

  inline def notImplemented: Consequence.Failure[Nothing] =
    notImplemented(SourcePositionMacro.position())

  def notImplemented(pos: SourcePosition): Consequence.Failure[Nothing] =
    Consequence.Failure(Conclusion.failNotImplemented(pos))
}
