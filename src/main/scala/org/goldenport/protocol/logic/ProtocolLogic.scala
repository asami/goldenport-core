package org.goldenport.protocol.logic

import org.goldenport.Consequence
import org.goldenport.protocol.Protocol
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.{OperationDefinition, ServiceDefinition}

/*
 * @since   Dec. 24, 2025
 * @version Dec. 28, 2025
 * @author  ASAMI, Tomoharu
 */
class ProtocolLogic(protocol: Protocol) {
  def makeOperationRequest(args: Array[String]): Consequence[OperationRequest] =
    for {
      req <- makeRequest(args)
      opreq <- makeOperationRequest(req)
    } yield opreq

  def makeOperationRequest(request: Request): Consequence[OperationRequest] = {
    _resolve_service(request) match {
      case Consequence.Success(service) =>
        _resolve_operation(service, request) match {
          case Consequence.Success(op) =>
            op.createOperationRequest(request)
          case Consequence.Failure(c) =>
            Consequence.Failure(c)
        }
      case Consequence.Failure(c) =>
        Consequence.Failure(c)
    }
  }

  private def _resolve_service(req: Request): Consequence[ServiceDefinition] =
    req.service match {
      case Some(name) =>
        protocol.services.services.find(_.name == name) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("service not found")
        }
      case None =>
        protocol.services.services.find(_.operations.operations.exists(_.name == req.operation)) match {
          case Some(service) => Consequence.success(service)
          case None => Consequence.failure("operation not found")
        }
    }

  private def _resolve_operation(
    service: ServiceDefinition,
    req: Request
  ): Consequence[OperationDefinition] =
    service.operations.operations.find(_.name == req.operation) match {
      case Some(op) => Consequence.success(op)
      case None =>
        Consequence
          .failArgumentValidationError
          .withOperation(req.operation)
          .build
    }

  def makeRequest(args: Array[String]): Consequence[Request] = {
    for {
      in <- protocol.takeIngress(args)
      req <- in.encode(args)
    } yield req
  }
}

object ProtocolLogic {
  def apply(protocol: org.goldenport.protocol.Protocol): ProtocolLogic =
    new ProtocolLogic(protocol)
}
