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
 *  version Apr.  8, 2026
 * @version Apr. 14, 2026
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

  inline def operationNotFound(
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    operationNotFound(name, facets, SourcePositionMacro.position())

  def operationNotFound(
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.NotFound),
      Descriptor.Facet.Operation(name) +: facets,
      pos
    )

  inline def operationConflict(
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    operationConflict(name, facets, SourcePositionMacro.position())

  def operationConflict(
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.Conflict),
      Descriptor.Facet.Operation(name) +: facets,
      pos
    )

  inline def operationIllegal(
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    operationIllegal(name, facets, SourcePositionMacro.position())

  inline def operationIllegal(
    name: String,
    message: String
  ): Consequence.Failure[Nothing] =
    operationIllegal(name, Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  def operationIllegal(
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy(Taxonomy.Category.Operation, Taxonomy.Symptom.Illegal),
      Descriptor.Facet.Operation(name) +: facets,
      pos
    )

  inline def operationInvalid(
    name: String,
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    operationInvalid(name, facets, SourcePositionMacro.position())

  inline def operationInvalid(
    name: String,
    message: String
  ): Consequence.Failure[Nothing] =
    operationInvalid(name, Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  def operationInvalid(
    name: String,
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy.operationInvalid,
      Descriptor.Facet.Operation(name) +: facets,
      pos
    )

  inline def componentInvalid(
    facets: Seq[Descriptor.Facet]
  ): Consequence.Failure[Nothing] =
    componentInvalid(facets, SourcePositionMacro.position())

  inline def componentInvalid(
    message: String
  ): Consequence.Failure[Nothing] =
    componentInvalid(Seq(Descriptor.Facet.Message(message)), SourcePositionMacro.position())

  inline def componentInvalid(
    e: Throwable
  ): Consequence.Failure[Nothing] =
    componentInvalid(Seq(Descriptor.Facet.Exception(e)), SourcePositionMacro.position())

  def componentInvalid(
    facets: Seq[Descriptor.Facet],
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(Taxonomy.componentInvalid, facets, pos)

  inline def componentNotFound(
    name: String
  ): Consequence.Failure[Nothing] =
    componentNotFound(name, SourcePositionMacro.position())

  def componentNotFound(
    name: String,
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy(Taxonomy.Category.Component, Taxonomy.Symptom.NotFound),
      Seq(Descriptor.Facet.Component(name)),
      pos
    )

  inline def serviceNotFound(
    name: String
  ): Consequence.Failure[Nothing] =
    serviceNotFound(name, SourcePositionMacro.position())

  def serviceNotFound(
    name: String,
    pos: SourcePosition
  ): Consequence.Failure[Nothing] =
    fail(
      Taxonomy(Taxonomy.Category.Service, Taxonomy.Symptom.NotFound),
      Seq(Descriptor.Facet.Service(name)),
      pos
    )

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
