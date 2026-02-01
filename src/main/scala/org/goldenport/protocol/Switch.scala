package org.goldenport.protocol

import org.goldenport.protocol.spec.ParameterDefinition

/*
 * @since   Oct.  5, 2018
 *  version Oct. 10, 2018
 *  version Mar. 16, 2025
 *  version Dec. 25, 2025
 * @version Feb.  1, 2026
 * @author  ASAMI, Tomoharu
 */
case class Switch(
  name: String,
  value: Boolean,
  spec: Option[ParameterDefinition]
) extends Parameter {
  def getArg: Option[String] = Option.when(value)(s"--$name")
}
