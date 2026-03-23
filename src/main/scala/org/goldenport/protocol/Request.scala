package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.text.Presentable
import org.simplemodeling.model.value.BaseContent
import org.goldenport.protocol.handler.ingress.ArgsIngress
import org.goldenport.protocol.spec.{OperationDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.text.Presentable
import org.goldenport.http.HttpRequest
import org.goldenport.record.Record

/*
 * @since   Oct.  4, 2018
 *  version Oct. 21, 2018
 *  version Feb. 24, 2019
 *  version Mar.  4, 2019
 *  version Feb. 16, 2020
 *  version May. 19, 2020
 *  version Apr. 25, 2021
 *  version Jan. 30, 2022
 *  version Feb.  1, 2022
 *  version Jan. 30, 2023
 *  version Jul. 23, 2023
 *  version Mar. 16, 2025
 *  version Apr.  2, 2025
 *  version Jun. 10, 2025
 *  version Dec. 24, 2025
 *  version Jan. 30, 2026
 *  version Feb. 19, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
case class Request(
  component: Option[String],
  service: Option[String],
  operation: String,
  arguments: List[Argument],
  switches: List[Switch],
  properties: List[Property],
  source: Option[Request.Source] = None
) extends Presentable {
  def name: String = Vector(component, service, Some(operation)).flatten.mkString(".")
  def args: List[String] = arguments.map(x => Presentable.print(x.value))
  override def display: String = name
  override def show: String =
    s"$name(args=${arguments.size}, switches=${switches.size}, props=${properties.size})"
  def print: String =
    s"Request(${component}, ${service}, ${operation}, ${arguments}, ${switches}, ${properties})"

  def withOperation(name: String) = copy(operation = name)

  def toArgs: Array[String] = {
    val a = Vector(name) ++ args ++ switches.flatMap(_.getArg) ++ properties.map(_.arg)
    a.toArray
  }

  def toSubCommand: Request = copy(
    operation = arguments.headOption.fold("")(_.printValue),
    arguments = arguments.drop(1).zipWithIndex.map(_adjust_name)
  )

  private val _Arg_Pattern = """arg(\d+)""".r

  private def _adjust_name(p: (Argument, Int)): Argument = p match {
    case (arg, index) =>
      arg.name match {
        case _Arg_Pattern(_) =>
          arg.copy(name = s"arg${index + 1}")
        case _ =>
          arg
      }
  }

  def toRecord: Record = {
    val props = (arguments ++ switches ++ properties).map(x => x.name -> x.value)
    Record.create(props)
  }
}

object Request {
  sealed abstract class Source
  object Source {
    case class Args(args: Array[String]) extends Source
    case class Http(http: HttpRequest) extends Source
  }

  def of(
    component: String,
    service: String,
    operation: String,
    arguments: List[Argument] = Nil,
    switches: List[Switch] = Nil,
    properties: List[Property] = Nil
  ): Request =
    Request(
      component = Some(component),
      service = Some(service),
      operation = operation,
      arguments = arguments,
      switches = switches,
      properties = properties
    )

  def ofHttpRequest(
    req: HttpRequest,
    component: String,
    service: String,
    operation: String,
    arguments: List[Argument] = Nil,
    switches: List[Switch] = Nil,
    properties: List[Property] = Nil
  ): Request =
    Request(
      component = Some(component),
      service = Some(service),
      operation = operation,
      arguments = arguments,
      switches = switches,
      properties = properties,
      source = Some(Source.Http(req))
    )


  def ofService(
    service: String,
    operation: String,
    arguments: List[Argument] = Nil,
    switches: List[Switch] = Nil,
    properties: List[Property] = Nil
  ): Request =
    Request(
      component = None,
      service = Some(service),
      operation = operation,
      arguments = arguments,
      switches = switches,
      properties = properties
    )

  def ofOperation(p: String): Request =
    Request(
      component = None,
      service = None,
      operation = p,
      arguments = Nil,
      switches = Nil,
      properties = Nil
    )

  def parseArgs(
    reqdef: RequestDefinition,
    args: Array[String]
  ): Consequence[Request] = {
    val op = OperationDefinition(
      content = BaseContent.simple("ingress"),
      request = reqdef,
      response = ResponseDefinition()
    )
    val ingress = ArgsIngress()
    ingress.encode(op, args)
  }
}
