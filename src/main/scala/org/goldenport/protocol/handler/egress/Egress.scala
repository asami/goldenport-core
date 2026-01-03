package org.goldenport.protocol.handler.egress

import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.protocol.Response

/*
 * @since   Dec. 28, 2025
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Egress[Out] {
  def kind: Egress.Kind[Out]
  def isAccept(res: Response): Boolean = true
  def egress(res: Response): Consequence[Out]
}

object Egress {
  sealed trait Kind[Out] {
    def name: String
  }
  object Kind {
    case object `String` extends Kind[String] {
      val name = "string"
    }
  }
}

final case class EgressCollection(
  egresses: Vector[Egress[?]] = Vector.empty
) {
  def ++(that: EgressCollection): EgressCollection =
    EgressCollection(this.egresses ++ that.egresses)

  def egress[Out](
    kind: Egress.Kind[Out],
    res: Response
  ): Consequence[Out] =
    egresses.collectFirst { case p if p.kind == kind => p } match {
      case Some(p) => p.asInstanceOf[Egress[Out]].egress(res)
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
