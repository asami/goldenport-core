package org.simplemodeling.cli.logic

import cats.syntax.all.*
import org.simplemodeling.Consequence
import org.simplemodeling.protocol.Request
import org.simplemodeling.protocol.operation.OperationRequest
import org.simplemodeling.protocol.ProtocolEngine
import org.simplemodeling.protocol.spec.ServiceDefinitionGroup
import org.simplemodeling.cli.parser.ArgsParser

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

  private def _resolve_service(req: Request): Consequence[org.simplemodeling.protocol.spec.ServiceDefinition] =
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
    service: org.simplemodeling.protocol.spec.ServiceDefinition,
    req: Request
  ): Consequence[org.simplemodeling.protocol.spec.OperationDefinition] =
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
    CliLogic(services, ProtocolEngine.create())
}
