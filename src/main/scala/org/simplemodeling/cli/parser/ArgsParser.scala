package org.simplemodeling.cli.parser

import org.simplemodeling.protocol.Request
import org.simplemodeling.protocol.Argument
import org.simplemodeling.protocol.Switch
import org.simplemodeling.protocol.Property
import org.simplemodeling.protocol.spec.ServiceDefinition

/*
 * @since   Dec. 23, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
class ArgsParser(config: ArgsParser.Config) {
  private val MaxParamLength = 2048

  /**
   * Parse CLI args into a protocol-level Request
   * using the given Service definition.
   */
  def parse(service: ServiceDefinition, args: List[String]): Request = {
    var arguments: List[Argument] = Nil
    var switches: List[Switch] = Nil
    var properties: List[Property] = Nil

    args.foreach { s =>
      if (s.startsWith("--") && s.contains("=")) {
        val parts = s.drop(2).split("=", 2)
        val key = if (parts.nonEmpty) parts(0) else ""
        val value = if (parts.length > 1) parts(1) else ""
        properties = properties :+ Property(key, value, None)
      } else if (s.startsWith("--")) {
        switches = switches :+ Switch(s.drop(2), true, None)
      } else {
        arguments = arguments :+ Argument(s, s, None)
      }
    }

    Request(
      service    = Some(service.name),
      operation  = args.headOption.getOrElse(""),
      arguments  = arguments,
      switches   = switches,
      properties = properties
    )
  }

  // Legacy validation-based implementation (kept for reference)
  // Validation is now handled in CliLogic using Consequence.

  // def parse0(
  //   service: Service,
  //   args: List[String]
  // ): Consequence[OperationRequest] =
  //   args match
  //     case Nil =>
  //       Consequence.failure("missing operation")

  //     case operationName :: params =>
  //       _validateOperation(service, operationName)
  //         .flatMap(_ => _validateParams(params))
  //         .map(_ =>
  //           OperationRequest(
  //             service   = service.name,
  //             operation = operationName,
  //             params    = params
  //           )
  //         )

  // private def _validateOperation(
  //   service: Service,
  //   operationName: String
  // ): Consequence[Unit] =
  //   if (operationName.trim.isEmpty)
  //     Consequence.failure("operation must not be empty")
  //   else if (!service.operations.exists(_.name == operationName))
  //     Consequence.failure(s"unknown operation: $operationName")
  //   else
  //     Consequence.Success(())

  // private def _validateParams(params: List[String]): Consequence[Unit] =
  //   if (params.exists(_.trim.isEmpty))
  //     Consequence.failure("parameter must not be empty")
  //   else if (params.exists(_.length > MaxParamLength))
  //     Consequence.failure(s"parameter must not exceed $MaxParamLength characters")
  //   else
  //     Consequence.Success(())
}

object ArgsParser {
  case class Config() {
  }
  object Config {
    val default = Config()
  }
}
