package org.goldenport.cli.logic

import cats.syntax.all.*
import org.goldenport.Consequence
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.{Protocol, ProtocolEngine}
import org.goldenport.protocol.handler.ProtocolHandler
import org.goldenport.protocol.handler.egress.EgressCollection
import org.goldenport.protocol.handler.ingress.IngressCollection
import org.goldenport.protocol.handler.projection.{CliHelpProjection, McpGetManifestProjection, OpenApiProjection, ProjectionCollection}
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.cli.parser.ArgsParser

/*
 * @since   Dec. 24, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
class CliLogic(
  serviceGroup: ServiceDefinitionGroup,
  protocol: ProtocolEngine
) {
  // serviceGroup provides the fixed set of services available to this CLI
  def makeRequest(args: Array[String]): Consequence[Request] = {
    val parser = new ArgsParser(ArgsParser.Config())
    val parsed = parser.parse(serviceGroup.services.head, args.toList)
    Consequence.Success(parsed)
  }

  def makeOperationRequest(req: Request): Consequence[OperationRequest] =
    _resolve_service(req) match {
      case Consequence.Success(service) =>
        _resolve_operation(service, req) match {
          case Consequence.Success(op) =>
            op.createOperationRequest(req)
          case Consequence.Failure(c) =>
            Consequence.Failure(c)
        }
      case Consequence.Failure(c) =>
        Consequence.Failure(c)
    }

  private def _resolve_service(req: Request): Consequence[org.goldenport.protocol.spec.ServiceDefinition] =
    req.service match {
      case Some(name) =>
        serviceGroup.services.find(_.name == name) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("service not found")
        }
      case None =>
        serviceGroup.services.find(_.operations.operations.exists(_.name == req.operation)) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("operation not found")
        }
    }

  private def _resolve_operation(
    service: org.goldenport.protocol.spec.ServiceDefinition,
    req: Request
  ): Consequence[org.goldenport.protocol.spec.OperationDefinition] =
    service.operations.operations.find(_.name == req.operation) match {
      case Some(op) => Consequence.success(op)
      case None => Consequence.failure("operation not found")
    }

  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    for {
      req <- makeRequest(args)
      opreq <- makeOperationRequest(req)
    } yield opreq
}

object CliLogic {
  def create(services: ServiceDefinitionGroup): CliLogic =
    val handler = ProtocolHandler(
      IngressCollection(Vector.empty),
      EgressCollection(Vector.empty),
      ProjectionCollection(Vector(new CliHelpProjection, new OpenApiProjection, new McpGetManifestProjection))
    )
    val protocol = Protocol(services, handler)
    CliLogic(services, ProtocolEngine.create(protocol))
}
