package org.goldenport.protocol.spec

import cats.data.NonEmptyVector
import org.goldenport.datatype.{I18nLabel, Name}
import org.goldenport.model.value.{BaseContent, DescriptiveAttributes, NameAttributes}
import org.goldenport.schema.{Multiplicity, ValueDomain, XString}

/*
 * @since   Oct.  6, 2018
 *  version Oct. 10, 2018
 *  version Feb. 16, 2020
 *  version Mar. 16, 2025
 *  version Dec. 25, 2025
 * @version Jan. 14, 2026
 * @author  ASAMI, Tomoharu
 */
case class RequestDefinition(
  parameters: List[ParameterDefinition] = Nil
) {
}

object RequestDefinition {
  val empty = new RequestDefinition()

  val script: RequestDefinition = RequestDefinition() // TODO

  def apply(): RequestDefinition = empty

  val curlLike: RequestDefinition =
    RequestDefinition(
      parameters = List(
        _curl_property("method", alias = Some("X"), multiplicity = Multiplicity.ZeroOne),
        _curl_property("data", alias = Some("d"), multiplicity = Multiplicity.ZeroOne),
        _curl_property("header", alias = Some("H"), multiplicity = Multiplicity.ZeroMore)
      )
    )

  private def _curl_property(
    name: String,
    alias: Option[String],
    multiplicity: Multiplicity
  ): ParameterDefinition = {
    val content = alias match {
      case Some(a) => _base_content_with_alias(name, a)
      case None => BaseContent.simple(name)
    }
    ParameterDefinition(
      content = content,
      kind = ParameterDefinition.Kind.Property,
      domain = ValueDomain(datatype = XString, multiplicity = multiplicity),
      default = ParameterDefinition.Default.Undefined,
      isMagicSequence = true,
      isEagerSequence = false
    )
  }

  private def _base_content_with_alias(
    name: String,
    alias: String
  ): BaseContent = {
    val nameattributes = NameAttributes(
      name = Name(name),
      label = None,
      title = None,
      code = None,
      alias = Some(NonEmptyVector.one(I18nLabel(alias))),
      slug = None,
      shortid = None
    )
    BaseContent(nameattributes, DescriptiveAttributes.empty)
  }
}
