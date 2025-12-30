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

case class EgressCollection(
  egresses: Vector[Egress[?]]
) {
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

abstract class JsonEgress extends Egress[Json] {
}
