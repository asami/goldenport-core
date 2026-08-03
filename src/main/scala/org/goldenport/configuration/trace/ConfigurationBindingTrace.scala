package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationBindingTraceValue

object ConfigurationBindingTraceValue {
  final case class Visible(value: ConfigurationValue) extends ConfigurationBindingTraceValue
  case object Redacted extends ConfigurationBindingTraceValue
}

final case class ConfigurationBindingTraceProvenance private[configuration] (
  origin: ConfigurationOrigin,
  layer: String,
  sourceIdentity: String,
  sourceIdentityTruncatedCount: Int,
  inputPath: Option[String],
  inputSpelling: Option[String],
  sourceRank: Int,
  sourceOrdinal: Int,
  evidence: Vector[String],
  omittedEvidenceCount: Int,
  isConfidential: Boolean
)

final case class ConfigurationBindingTraceEntry[T] private[configuration] (
  target: T,
  value: ConfigurationBindingTraceValue,
  provenance: ConfigurationBindingTraceProvenance
)

final class ConfigurationBindingExplanation[T] private[configuration] (
  private[configuration] val parameter: ConfigurationParameter[?],
  val parameterId: CanonicalParameterId,
  val entries: Vector[ConfigurationBindingTraceEntry[T]],
  val omittedEntryCount: Int
) {
  def effective: ConfigurationBindingTraceEntry[T] =
    entries.head
}

final class ConfigurationBindingTrace[T] private (
  val entries: Vector[ConfigurationBindingExplanation[T]],
  private val _explanations: Map[CanonicalParameterId, ConfigurationBindingExplanation[T]]
) {
  def explain[A](
    parameter: ConfigurationParameter[A]
  ): Consequence[Option[ConfigurationBindingExplanation[T]]] =
    if (parameter == null)
      Consequence.configurationInvalid("configuration binding trace lookup parameter is required")
    else
      _explanations.get(parameter.id) match {
        case None => Consequence.success(None)
        case Some(explanation) if explanation.parameter.asInstanceOf[AnyRef] eq parameter.asInstanceOf[AnyRef] =>
          Consequence.success(Some(explanation))
        case Some(_) =>
          Consequence.configurationInvalid("configuration binding trace lookup requires the original parameter witness")
      }
}

object ConfigurationBindingTrace {
  private val _maximum_entries = 16
  private val _maximum_source_identity_code_units = 256

  def from[T](
    bindings: ConfigurationBindingCollection[T]
  ): Consequence[ConfigurationBindingTrace[T]] =
    if (bindings == null)
      Consequence.configurationInvalid("configuration binding trace requires resolved bindings")
    else
      bindings._bindings_for_trace.sortBy(_.parameter.id.value).foldLeft(
        Consequence.success(Vector.empty[ConfigurationBindingExplanation[T]])
      ) { (acc, binding) =>
        for {
          explanations <- acc
          explanation <- _explanation(binding)
        } yield explanations :+ explanation
      }.map { explanations =>
        new ConfigurationBindingTrace(explanations, explanations.map(x => x.parameterId -> x).toMap)
      }

  private def _explanation[T](
    binding: ConfigurationBinding[?, T]
  ): Consequence[ConfigurationBindingExplanation[T]] =
    _chain(binding).foldLeft(Consequence.success(Vector.empty[ConfigurationBindingTraceEntry[T]])) {
      (acc, item) =>
        for {
          entries <- acc
          entry <- _entry(item)
        } yield entries :+ entry
    }.map { entries =>
      new ConfigurationBindingExplanation(
        binding.parameter,
        binding.parameter.id,
        entries.take(_maximum_entries),
        entries.size - entries.take(_maximum_entries).size
      )
    }

  private def _chain[T](
    binding: ConfigurationBinding[?, T]
  ): Vector[ConfigurationBinding[?, T]] = {
    @annotation.tailrec
    def _collect_(
      current: Option[ConfigurationBinding[?, T]],
      entries: List[ConfigurationBinding[?, T]]
    ): Vector[ConfigurationBinding[?, T]] =
      current match {
        case Some(item) => _collect_(item.overridden, item :: entries)
        case None => entries.reverse.toVector
      }

    _collect_(Some(binding), Nil)
  }

  private def _entry[T](
    binding: ConfigurationBinding[?, T]
  ): Consequence[ConfigurationBindingTraceEntry[T]] =
    _value(binding).map { value =>
      ConfigurationBindingTraceEntry(binding.target, value, _provenance(binding.provenance))
    }

  private def _value[T](
    binding: ConfigurationBinding[?, T]
  ): Consequence[ConfigurationBindingTraceValue] =
    if (binding.provenance.isConfidential)
      Consequence.success(ConfigurationBindingTraceValue.Redacted)
    else
      binding._encoded_value.map(ConfigurationBindingTraceValue.Visible.apply)

  private def _provenance(
    provenance: ConfigurationProvenance
  ): ConfigurationBindingTraceProvenance = {
    val sourceidentity = _bound_source_identity(provenance.sourceIdentity)
    ConfigurationBindingTraceProvenance(
      provenance.origin,
      provenance.layer,
      sourceidentity,
      provenance.sourceIdentity.length - sourceidentity.length,
      provenance.inputPath,
      provenance.inputSpelling,
      provenance.sourceRank,
      provenance.sourceOrdinal,
      provenance.evidence,
      provenance.omittedEvidenceCount,
      provenance.isConfidential
    )
  }

  private def _bound_source_identity(value: String): String =
    if (value.length <= _maximum_source_identity_code_units)
      value
    else {
      val boundary =
        if (Character.isHighSurrogate(value.charAt(_maximum_source_identity_code_units - 1)) &&
          Character.isLowSurrogate(value.charAt(_maximum_source_identity_code_units)))
          _maximum_source_identity_code_units - 1
        else
          _maximum_source_identity_code_units
      value.substring(0, boundary)
    }
}
