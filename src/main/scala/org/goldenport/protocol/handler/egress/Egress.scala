package org.goldenport.protocol.handler.egress

import io.circe.Json
import org.goldenport.{Conclusion, Consequence}

/*
 * @since   Dec. 28, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
trait EgressKind[Out] {
  def name: String
}

abstract class Egress[Out] {
  def kind: EgressKind[Out]
  def egress(c: Conclusion): Consequence[Out]
}

final case class EgressCollection(
  egresses: Vector[Egress[?]] = Vector.empty
) {
  def ++(that: EgressCollection): EgressCollection =
    EgressCollection(this.egresses ++ that.egresses)

  def egress[Out](
    kind: EgressKind[Out],
    c: Conclusion
  ): Consequence[Out] =
    egresses.collectFirst { case p if p.kind == kind => p } match {
      case Some(p) => p.asInstanceOf[Egress[Out]].egress(c)
      case None =>
        Consequence
          .failArgumentMissing
          .withInput(kind.name)
          .build
    }
}

object EgressCollection {

  /** Canonical empty instance */
  val empty: EgressCollection =
    new EgressCollection(Vector.empty)

  /** Binary-compatible zero-arg constructor */
  def apply(): EgressCollection =
    empty
}

abstract class JsonEgress extends Egress[Json] {
}
