package org.simplemodeling.cli.runner

import org.simplemodeling.cli.model.Service
import org.simplemodeling.cli.model.Operation
import org.simplemodeling.cli.model.ServiceRegistry

/*
 * @since   Dec. 22, 2025
 * @version Dec. 22, 2025
 * @author  ASAMI, Tomoharu
 */
object NameResolver:
  def resolve(
    args: List[String],
    registry: ServiceRegistry
  ): Option[(Service, Operation, List[String])] =
    None

class MiniCliRunner(registry: ServiceRegistry):
  def run(args: Array[String]): Int =
    println(s"MiniCliRunner: args=${args.toList}")
    0
