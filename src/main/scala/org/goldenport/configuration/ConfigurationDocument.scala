package org.goldenport.configuration

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationDocument

object ConfigurationDocument {
  final case class Scalar(value: ConfigurationValue) extends ConfigurationDocument

  final case class Sequence(values: Vector[ConfigurationDocument]) extends ConfigurationDocument

  final case class Object(fields: Vector[Field]) extends ConfigurationDocument

  final case class Field(
    name: String,
    value: ConfigurationDocument
  )
}
