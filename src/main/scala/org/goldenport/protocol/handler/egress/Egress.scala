package org.goldenport.protocol.handler.egress

import java.nio.charset.StandardCharsets
import io.circe.Json
import org.goldenport.Consequence
import org.goldenport.bag.Bag
import org.goldenport.http.{ContentType, HttpResponse, HttpStatus, MimeType, StringResponse}
import org.goldenport.model.value.BaseContent
import org.goldenport.protocol.Response
import org.goldenport.protocol.spec.{OperationDefinition, RequestDefinition, ResponseDefinition}

/*
 * @since   Dec. 28, 2025
 *  version Jan.  2, 2026
 * @version Jan.  7, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class Egress[Out] {
  def kind: Egress.Kind[Out]
  def outputClass: Class[Out] = classOf[Any].asInstanceOf[Class[Out]]
  def isAccept(res: Response): Boolean = true
  def egress(res: Response): Consequence[Out]

  def encode(op: OperationDefinition, res: Response): HttpResponse = {
    val _ = op
    val _ = res
    throw new UnsupportedOperationException("Egress.encode is supported only by HTTP egress")
  }
}

object Egress {
  sealed trait Kind[Out] {
    def name: String
  }
  object Kind {
    case object `String` extends Kind[String] {
      val name = "string"
    }
    case object Http extends Kind[HttpResponse] {
      val name = "http"
    }
  }
}

final case class EgressCollection(
  egresses: Vector[Egress[?]] = Vector.empty
) {
  def ++(that: EgressCollection): EgressCollection =
    EgressCollection(this.egresses ++ that.egresses)

  def findByOutput[Out](clazz: Class[Out]): Option[Egress[Out]] =
    egresses.collectFirst {
      case egress if egress.outputClass == clazz =>
        egress.asInstanceOf[Egress[Out]]
    }

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
  override def outputClass: Class[Json] = classOf[Json]
}

final case class RestEgress() extends Egress[HttpResponse] {
  override val kind: Egress.Kind[HttpResponse] = Egress.Kind.Http
  override val outputClass: Class[HttpResponse] = classOf[HttpResponse]

  override def egress(res: Response): Consequence[HttpResponse] =
    Consequence.success(encode(_default_operation_definition, res))

  override def encode(
    op: OperationDefinition,
    res: Response
  ): HttpResponse = {
    val _ = op
    res match {
      case Response.Void() =>
        _ok_text("")
      case Response.Json(value) =>
        _ok_json(value)
      case Response.Scalar(value) =>
        _ok_text(value.toString)
      case _ =>
        StringResponse(
          HttpStatus.InternalServerError,
          _text_content_type,
          Bag.text("unsupported response")
        )
    }
  }

  private val _text_content_type: ContentType =
    ContentType(MimeType("text/plain"), Some(StandardCharsets.UTF_8))

  private val _json_content_type: ContentType =
    ContentType(MimeType("application/json"), Some(StandardCharsets.UTF_8))

  private def _ok_text(value: String): HttpResponse =
    StringResponse(HttpStatus.Ok, _text_content_type, Bag.text(value))

  private def _ok_json(value: String): HttpResponse =
    StringResponse(HttpStatus.Ok, _json_content_type, Bag.text(value))

  private val _default_operation_definition: OperationDefinition =
    OperationDefinition(
      BaseContent.simple("egress"),
      RequestDefinition.empty,
      ResponseDefinition()
    )
}
