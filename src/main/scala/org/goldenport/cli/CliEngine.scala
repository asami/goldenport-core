package org.goldenport.cli

import org.goldenport.cli.logic.CliLogic
import org.goldenport.protocol.Request
import org.goldenport.protocol.operation.OperationRequest
import org.goldenport.protocol.spec.ServiceDefinitionGroup
import org.goldenport.Consequence

/* CliEngine – Minimal Wiring Implementation
 * 
 * Purpose:
 *  - Application-facing entry point
 *  - Wire ServiceDefinition / OperationDefinition
 *  - Produce syntactic Request and semantic OperationRequest
 *  - Execution is explicitly out of scope (reserved for execute phase)
 */
/*
 * @since   Dec. 26, 2025
 * @version Dec. 26, 2025
 * @author  ASAMI, Tomoharu
 */
final class CliEngine(
  config: CliEngine.Config,
  specification: CliEngine.Specification
) {
  private val _logic: CliLogic = CliLogic.create(specification.services)

  /**
   * Entry point for CLI applications.
   *
   * Responsibility:
   * - Convert raw CLI inputs into a syntactic Request
   * - Resolve service / operation
   * - Delegate semantic interpretation to OperationDefinition
   *
   * MUST NOT:
   * - Execute the operation
   * - Perform semantic validation itself
   */
  def makeRequest(
    args: Seq[String],
    switches: Map[String, String] = Map.empty,
    properties: Map[String, String] = Map.empty
  ): Consequence[OperationRequest] = {
    val _ = switches
    val _ = properties
    for {
      request <- _logic.makeRequest(args.toArray)
      opreq <- _logic.makeOperationRequest(request)
    } yield opreq
  }
}

object CliEngine {

  /**
   * Runtime configuration (logging, mode, etc.)
   * Intentionally minimal at this stage.
   */
  case class Config()

  /**
   * Static specification of available services.
   *
   * Provided by application at startup.
   */
  case class Specification(
    services: ServiceDefinitionGroup
  )
}
