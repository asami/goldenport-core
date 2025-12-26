package org.goldenport.protocol

import org.goldenport.protocol.spec.ParameterDefinition

/*
 * @since   Oct.  5, 2018
 *  version May. 19, 2019
 *  version Feb. 16, 2020
 *  version Apr. 25, 2021
 *  version Jan. 30, 2022
 *  version Jul. 22, 2023
 *  version Mar. 16, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
case class Argument(
  name: String,
  value: Any,
  spec: Option[ParameterDefinition]
) extends Parameter { // with ConsequenceValueHelper {
}
