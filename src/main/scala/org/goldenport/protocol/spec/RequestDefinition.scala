package org.goldenport.protocol.spec

/*
 * @since   Oct.  6, 2018
 *  version Oct. 10, 2018
 *  version Feb. 16, 2020
 *  version Mar. 16, 2025
 *  version Dec. 25, 2025
 * @version Jan.  2, 2026
 * @author  ASAMI, Tomoharu
 */
case class RequestDefinition(
  parameters: List[ParameterDefinition] = Nil
) {
}

object RequestDefinition {
  val empty = new RequestDefinition()

  def apply(): RequestDefinition = empty
}
