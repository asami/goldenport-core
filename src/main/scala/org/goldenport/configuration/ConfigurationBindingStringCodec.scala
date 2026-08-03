package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingReference[T] private (
  val parameterId: CanonicalParameterId,
  val target: T
)

object ConfigurationBindingReference {
  def create[T](
    parameterId: CanonicalParameterId,
    target: T
  ): Consequence[ConfigurationBindingReference[T]] =
    if (parameterId == null)
      Consequence.configurationInvalid("configuration binding reference parameter id is required")
    else if (target == null)
      Consequence.configurationInvalid("configuration binding reference target is required")
    else
      Consequence.success(new ConfigurationBindingReference(parameterId, target))
}

trait ConfigurationBindingQualifierCodec[T] {
  /** None denotes the unqualified (global) target. */
  def decode(value: Option[String]): Consequence[T]

  /** None emits the historical bare canonical parameter identity. */
  def encode(value: T): Consequence[Option[String]]
}

final class ConfigurationBindingStringCodec[T] private (
  qualifierCodec: ConfigurationBindingQualifierCodec[T]
) {
  def decode(value: String): Consequence[ConfigurationBindingReference[T]] =
    _split(value).flatMap { case (qualifier, parametertext) =>
      for {
        parameterid <- CanonicalParameterId.parse(parametertext)
        target <- qualifierCodec.decode(qualifier)
        reference <- ConfigurationBindingReference.create(parameterid, target)
      } yield reference
    }

  def encode(value: ConfigurationBindingReference[T]): Consequence[String] =
    if (value == null)
      Consequence.configurationInvalid("configuration binding reference is required")
    else
      qualifierCodec.encode(value.target).flatMap {
        case None => Consequence.success(value.parameterId.value)
        case Some(qualifier) if _is_valid_qualifier(qualifier) =>
          Consequence.success(s"@$qualifier:${value.parameterId.value}")
        case _ => Consequence.configurationInvalid("configuration binding qualifier is invalid")
      }

  private def _split(value: String): Consequence[(Option[String], String)] =
    Option(value).filter(_.nonEmpty).fold[Consequence[(Option[String], String)]](
      Consequence.configurationInvalid("configuration binding string is required")
    ) { text =>
      if (text.startsWith("@")) {
        val colon = text.indexOf(':')
        if (colon <= 1 || colon != text.lastIndexOf(':') || colon == text.length - 1)
          Consequence.configurationInvalid("configuration binding string is malformed")
        else
          Consequence.success(Some(text.substring(1, colon)) -> text.substring(colon + 1))
      } else if (text.contains('@') || text.contains(':')) {
        Consequence.configurationInvalid("configuration binding string is malformed")
      } else {
        Consequence.success(None -> text)
      }
    }

  private def _is_valid_qualifier(value: String): Boolean =
    Option(value).exists(x => x.nonEmpty && !x.contains('@') && !x.contains(':'))
}

object ConfigurationBindingStringCodec {
  def create[T](
    qualifierCodec: ConfigurationBindingQualifierCodec[T]
  ): Consequence[ConfigurationBindingStringCodec[T]] =
    Option(qualifierCodec).fold[Consequence[ConfigurationBindingStringCodec[T]]](
      Consequence.configurationInvalid("configuration binding qualifier codec is required")
    )(x => Consequence.success(new ConfigurationBindingStringCodec(x)))
}
