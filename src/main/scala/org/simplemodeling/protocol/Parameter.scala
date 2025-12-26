package org.goldenport.protocol

import org.goldenport.protocol.spec.ParameterDefinition

/*
 * @since   Mar.  1, 2025
 *  version Mar.  9, 2025
 * @version Dec. 25, 2025
 * @author  ASAMI, Tomoharu
 */
trait Parameter {
  def name: String
  def value: Any
  def spec: Option[ParameterDefinition]

  // def asString: String = AnyUtils.toString(value)
  // def asUrl: URL = AnyUtils.toUrl(value)
  // def asUrlList: List[URL] = value match {
  //   case m: URL => List(m)
  //   case m: URI => List(m.toURL)
  //   case m =>
  //     val s = AnyUtils.toString(m)
  //     val xs = Strings.totokens(";")
  //     xs.map(AnyUtils.toUrl)
  // }

  // def asIntOrString: Either[Int, String] = NumberUtils.optionInt(value) match {
  //   case Some(s) => Left(s)
  //   case None => Right(asString)
  // }

  // def toInputText: String = BufferBag.fromUri(value.toString).toText
}
