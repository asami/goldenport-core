package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationSourceAdmission[A] private (
  val origin: ConfigurationOrigin,
  val layer: String,
  val sourceIdentity: String,
  val sourceRank: Int,
  val collisionDomain: String,
  private val _load: () => Consequence[A],
  val sourceType: Option[String]
) {
  private[configuration] def load(): Consequence[A] =
    _load()
}

object ConfigurationSourceAdmission {
  def create[A](
    origin: ConfigurationOrigin,
    layer: String,
    sourceIdentity: String,
    sourceRank: Int,
    collisionDomain: String,
    load: () => Consequence[A],
    sourceType: Option[String] = None
  ): Consequence[ConfigurationSourceAdmission[A]] =
    if (origin == null)
      Consequence.configurationInvalid("configuration source origin is required")
    else if (!_is_required_text(layer))
      Consequence.configurationInvalid("configuration source layer is required")
    else if (!_is_required_text(sourceIdentity))
      Consequence.configurationInvalid("configuration source identity is required")
    else if (sourceRank < 0)
      Consequence.configurationInvalid("configuration source rank is invalid")
    else if (!_is_required_text(collisionDomain))
      Consequence.configurationInvalid("configuration source collision domain is required")
    else if (load == null)
      Consequence.configurationInvalid("configuration source loader is required")
    else if (sourceType == null || sourceType.exists(x => !_is_required_text(x)))
      Consequence.configurationInvalid("configuration source type is invalid")
    else
      Consequence.success(
        new ConfigurationSourceAdmission(
          origin,
          layer,
          sourceIdentity,
          sourceRank,
          collisionDomain,
          load,
          sourceType
        )
      )

  private def _is_required_text(value: String): Boolean =
    Option(value).exists(_.nonEmpty)
}

final class ConfigurationSourceSnapshot[A] private (
  val origin: ConfigurationOrigin,
  val layer: String,
  val sourceIdentity: String,
  val sourceRank: Int,
  val sourceOrdinal: Int,
  val collisionDomain: String,
  val sourceType: Option[String],
  val value: A
)

object ConfigurationSourceSnapshot {
  private[configuration] def create[A](
    admission: ConfigurationSourceAdmission[A],
    sourceOrdinal: Int,
    value: A
  ): Consequence[ConfigurationSourceSnapshot[A]] =
    if (admission == null || value == null || sourceOrdinal < 0)
      Consequence.configurationInvalid("configuration source snapshot is invalid")
    else
      Consequence.success(
        new ConfigurationSourceSnapshot(
          admission.origin,
          admission.layer,
          admission.sourceIdentity,
          admission.sourceRank,
          sourceOrdinal,
          admission.collisionDomain,
          admission.sourceType,
          value
        )
      )
}

final class ConfigurationSourceSnapshots[A] private (
  val snapshots: Vector[ConfigurationSourceSnapshot[A]]
)

object ConfigurationSourceSnapshots {
  private val _empty = new ConfigurationSourceSnapshots[Nothing](Vector.empty)

  def empty[A]: ConfigurationSourceSnapshots[A] =
    _empty.asInstanceOf[ConfigurationSourceSnapshots[A]]

  def apply[A](): ConfigurationSourceSnapshots[A] =
    empty

  def load[A](
    admissions: Vector[ConfigurationSourceAdmission[A]]
  ): Consequence[ConfigurationSourceSnapshots[A]] =
    if (admissions == null || admissions.exists(_ == null))
      Consequence.configurationInvalid("configuration source admissions are invalid")
    else
      admissions.zipWithIndex.foldLeft(Consequence.success(Vector.empty[ConfigurationSourceSnapshot[A]])) {
        case (acc, (admission, ordinal)) =>
          for {
            snapshots <- acc
            value <- admission.load()
            snapshot <- ConfigurationSourceSnapshot.create(admission, ordinal, value)
          } yield snapshots :+ snapshot
      }.map(xs => new ConfigurationSourceSnapshots(xs))

  def fromValues[A](
    values: Vector[(ConfigurationSourceAdmission[A], A)]
  ): Consequence[ConfigurationSourceSnapshots[A]] =
    if (values == null || values.exists { case (admission, value) => admission == null || value == null })
      Consequence.configurationInvalid("configuration source snapshot values are invalid")
    else
      values.zipWithIndex.foldLeft(Consequence.success(Vector.empty[ConfigurationSourceSnapshot[A]])) {
        case (acc, ((admission, value), ordinal)) =>
          for {
            snapshots <- acc
            snapshot <- ConfigurationSourceSnapshot.create(admission, ordinal, value)
          } yield snapshots :+ snapshot
      }.map(xs => new ConfigurationSourceSnapshots(xs))
}
