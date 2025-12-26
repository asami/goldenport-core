package org.goldenport.protocol

import org.goldenport.protocol.spec.ParameterDefinition

/*
 * @since   Oct.  5, 2018
 *  version Oct.  8, 2018
 *  version Apr. 25, 2021
 *  version Mar. 16, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class Property(
  name: String,
  value: Any,
  spec: Option[ParameterDefinition]
) extends Parameter {
}
