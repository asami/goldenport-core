package org.goldenport.sie.cli

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.protocol.Argument
import org.goldenport.protocol.operation.OperationRequest

/*
 * @since   Dec. 23, 2025
 * @version Dec. 23, 2025
 * @author  ASAMI, Tomoharu
 */
object ArgParser:
  private val MAX_QUERY_LENGTH = 2048

  def parse(args: List[String]): Consequence[OperationRequest] =
    (_parse_command(args), _parse_query(args)).mapN { (_, query) =>
      OperationRequest(
        service = Some("sie"),
        operation = "query",
        arguments = List(Argument("query", query, None)),
        switches = Nil,
        properties = Nil
      )
    }

  private def _parse_command(args: List[String]): Consequence[String] =
    args match
      case "query" :: _ =>
        Consequence.success("query")
      case Nil =>
        _failure
      case _ :: _ =>
        _failure

  private def _parse_query(args: List[String]): Consequence[String] =
    args match
      case "query" :: query :: Nil =>
        _validate_query(query)
      case "query" :: _ =>
        _failure
      case Nil =>
        _failure
      case _ :: _ =>
        _failure

  private def _validate_query(query: String): Consequence[String] =
    val trimmed = query.trim
    if trimmed.isEmpty then
      _failure
    else if trimmed.length > MAX_QUERY_LENGTH then
      _failure
    else
      Consequence.success(trimmed)

  private def _failure[T]: Consequence[T] =
    Consequence.Failure(null)
