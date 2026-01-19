package org.goldenport.protocol

import org.goldenport.Consequence
import org.goldenport.text.Presentable
import org.goldenport.model.value.BaseContent
import org.goldenport.protocol.handler.ingress.ArgsIngress
import org.goldenport.protocol.spec.{OperationDefinition, RequestDefinition, ResponseDefinition}
import org.goldenport.text.Presentable

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
 * @version Jan. 20, 2026
 * @author  ASAMI, Tomoharu
 */
case class Request(
  component: Option[String],
  service: Option[String],
  operation: String,
  arguments: List[Argument],
  switches: List[Switch],
  properties: List[Property]
) extends Presentable {
  def name: String = Vector(component, service, Some(operation)).flatten.mkString(".")
  def args: List[String] = arguments.map(x => Presentable.print(x.value))
  override def display: String = name
  override def show: String =
    s"$name(args=${arguments.size}, switches=${switches.size}, props=${properties.size})"
  def print: String =
    s"Request(${component}, ${service}, ${operation}, ${arguments}, ${switches}, ${properties})"
}

object Request {
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
