package org.goldenport.protocol.handler.projection

import org.goldenport.Consequence
import org.goldenport.protocol.spec.ServiceDefinitionGroup

/*
 * @since   Dec. 30, 2025
 * @version Dec. 30, 2025
 * @author  ASAMI, Tomoharu
 */
trait ProjectionKind[Out] {
  def name: String
}

abstract class Projection[Out] {
  def kind: ProjectionKind[Out]
  def project(defs: ServiceDefinitionGroup): Consequence[Out]
}

final case class ProjectionCollection(
  projections: Vector[Projection[?]] = Vector.empty
) {
  def ++(that: ProjectionCollection): ProjectionCollection =
    ProjectionCollection(this.projections ++ that.projections)

  def project[Out](
    kind: ProjectionKind[Out],
    defs: ServiceDefinitionGroup
  ): Consequence[Out] =
    projections.collectFirst { case p if p.kind == kind => p } match {
      case Some(p) => p.asInstanceOf[Projection[Out]].project(defs)
      case None =>
        Consequence
          .failArgumentMissing
          .withInput(kind.name)
          .build
    }

  def projectByName(
    name: String,
    defs: ServiceDefinitionGroup
  ): Consequence[Any] =
    projections.collectFirst { case p if p.kind.name == name => p } match {
      case Some(p) => p.asInstanceOf[Projection[Any]].project(defs)
      case None =>
        Consequence
          .failArgumentMissing
          .withInput(name)
          .build
    }
}

object ProjectionCollection {

  /** Canonical empty instance */
  val empty: ProjectionCollection =
    new ProjectionCollection(Vector.empty)

  /** Binary-compatible zero-arg constructor */
  def apply(): ProjectionCollection =
    empty
}
