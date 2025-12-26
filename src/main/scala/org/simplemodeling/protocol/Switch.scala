package org.simplemodeling.protocol

import org.simplemodeling.protocol.spec.ParameterDefinition

/*
 * @since   Oct.  5, 2018
 *  version Oct. 10, 2018
 *  version Mar. 16, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class Switch(
  name: String,
  value: Boolean,
  spec: Option[ParameterDefinition]
) extends Parameter {
}
