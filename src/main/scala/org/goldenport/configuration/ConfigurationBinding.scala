package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  2, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class CanonicalParameterId private (
  val value: String
) {
  override def equals(other: Any): Boolean =
    other match {
      case that: CanonicalParameterId => value == that.value
      case _ => false
    }

  override def hashCode(): Int =
    value.hashCode

  override def toString: String =
    value
}

object CanonicalParameterId {
  private val _pattern = "[a-z][a-z0-9-]*(\\.[a-z][a-z0-9-]*)+".r

  def parse(value: String): Consequence[CanonicalParameterId] =
    Option(value).filter(_pattern.matches).fold[Consequence[CanonicalParameterId]](
      Consequence.configurationInvalid("canonical configuration parameter id is invalid")
    )(x => Consequence.success(new CanonicalParameterId(x)))
}

trait ConfigurationValueCodec[A] {
  def decode(value: ConfigurationValue): Consequence[A]
  def encode(value: A): Consequence[ConfigurationValue]
}

object ConfigurationValueCodec {
  val string: ConfigurationValueCodec[String] =
    new ConfigurationValueCodec[String] {
      def decode(value: ConfigurationValue): Consequence[String] =
        value match {
          case ConfigurationValue.StringValue(v) => Consequence.success(v)
          case _ => Consequence.configurationInvalid("configuration value requires a string")
        }

      def encode(value: String): Consequence[ConfigurationValue] =
        Option(value).fold[Consequence[ConfigurationValue]](
          Consequence.configurationInvalid("configuration string value is required")
        )(x => Consequence.success(ConfigurationValue.StringValue(x)))
    }

  val boolean: ConfigurationValueCodec[Boolean] =
    new ConfigurationValueCodec[Boolean] {
      def decode(value: ConfigurationValue): Consequence[Boolean] =
        value match {
          case ConfigurationValue.BooleanValue(v) => Consequence.success(v)
          case _ => Consequence.configurationInvalid("configuration value requires a boolean")
        }

      def encode(value: Boolean): Consequence[ConfigurationValue] =
        Consequence.success(ConfigurationValue.BooleanValue(value))
    }

  val bigDecimal: ConfigurationValueCodec[BigDecimal] =
    new ConfigurationValueCodec[BigDecimal] {
      def decode(value: ConfigurationValue): Consequence[BigDecimal] =
        value match {
          case ConfigurationValue.NumberValue(v) => Consequence.success(v)
          case _ => Consequence.configurationInvalid("configuration value requires a number")
        }

      def encode(value: BigDecimal): Consequence[ConfigurationValue] =
        Option(value).fold[Consequence[ConfigurationValue]](
          Consequence.configurationInvalid("configuration number value is required")
        )(x => Consequence.success(ConfigurationValue.NumberValue(x)))
    }
}

final class ConfigurationParameter[A] private (
  val id: CanonicalParameterId,
  val codec: ConfigurationValueCodec[A]
)

object ConfigurationParameter {
  def create[A](
    id: CanonicalParameterId,
    codec: ConfigurationValueCodec[A]
  ): Consequence[ConfigurationParameter[A]] =
    if (id == null)
      Consequence.configurationInvalid("configuration parameter id is required")
    else if (codec == null)
      Consequence.configurationInvalid("configuration parameter codec is required")
    else
      Consequence.success(new ConfigurationParameter(id, codec))
}

final class ConfigurationProvenance private (
  val origin: ConfigurationOrigin,
  val layer: String,
  val sourceIdentity: String,
  val inputPath: Option[String],
  val inputSpelling: Option[String],
  val sourceRank: Int,
  val sourceOrdinal: Int,
  val evidence: Vector[String],
  val omittedEvidenceCount: Int,
  val isConfidential: Boolean,
  val sourceType: Option[String]
)

object ConfigurationProvenance {
  private val _maximum_evidence_entries = 16
  private val _maximum_evidence_code_units = 256

  def create(
    origin: ConfigurationOrigin,
    layer: String,
    sourceIdentity: String,
    inputPath: Option[String],
    inputSpelling: Option[String],
    sourceRank: Int,
    sourceOrdinal: Int,
    evidence: Vector[String],
    isConfidential: Boolean,
    sourceType: Option[String] = None
  ): Consequence[ConfigurationProvenance] =
    if (origin == null)
      Consequence.configurationInvalid("configuration provenance origin is required")
    else if (!_is_required_text(layer))
      Consequence.configurationInvalid("configuration provenance layer is required")
    else if (!_is_required_text(sourceIdentity))
      Consequence.configurationInvalid("configuration provenance source identity is required")
    else if (!_is_optional_text(inputPath))
      Consequence.configurationInvalid("configuration provenance input path is invalid")
    else if (!_is_optional_text(inputSpelling))
      Consequence.configurationInvalid("configuration provenance input spelling is invalid")
    else if (sourceRank < 0 || sourceOrdinal < 0)
      Consequence.configurationInvalid("configuration provenance ordering is invalid")
    else if (evidence == null || !evidence.forall(_is_required_text))
      Consequence.configurationInvalid("configuration provenance evidence is invalid")
    else if (sourceType == null || sourceType.exists(x => !_is_required_text(x)))
      Consequence.configurationInvalid("configuration provenance source type is invalid")
    else {
      val retained = evidence.take(_maximum_evidence_entries).map(_bound_evidence)
      Consequence.success(
        new ConfigurationProvenance(
          origin,
          layer,
          sourceIdentity,
          inputPath,
          inputSpelling,
          sourceRank,
          sourceOrdinal,
          retained,
          evidence.size - retained.size,
          isConfidential,
          sourceType
        )
      )
    }

  private def _is_required_text(value: String): Boolean =
    Option(value).exists(_.nonEmpty)

  private def _is_optional_text(value: Option[String]): Boolean =
    value != null && value.forall(_is_required_text)

  private def _bound_evidence(value: String): String =
    if (value.length <= _maximum_evidence_code_units)
      value
    else {
      val boundary =
        if (Character.isHighSurrogate(value.charAt(_maximum_evidence_code_units - 1)) &&
          Character.isLowSurrogate(value.charAt(_maximum_evidence_code_units)))
          _maximum_evidence_code_units - 1
        else
          _maximum_evidence_code_units
      value.substring(0, boundary)
    }
}

final class ConfigurationBindingCandidate[A, T] private (
  val parameter: ConfigurationParameter[A],
  val target: T,
  val value: A,
  val provenance: ConfigurationProvenance
)

object ConfigurationBindingCandidate {
  def create[A, T](
    parameter: ConfigurationParameter[A],
    target: T,
    value: A,
    provenance: ConfigurationProvenance
  ): Consequence[ConfigurationBindingCandidate[A, T]] =
    if (parameter == null)
      Consequence.configurationInvalid("configuration binding parameter is required")
    else if (target == null)
      Consequence.configurationInvalid("configuration binding target is required")
    else if (value == null)
      Consequence.configurationInvalid("configuration binding value is required")
    else if (provenance == null)
      Consequence.configurationInvalid("configuration binding provenance is required")
    else
      parameter.codec.encode(value).map(_ => new ConfigurationBindingCandidate(parameter, target, value, provenance))
}

final class ConfigurationBinding[A, T] private (
  val parameter: ConfigurationParameter[A],
  val target: T,
  val value: A,
  val provenance: ConfigurationProvenance,
  val overridden: Option[ConfigurationBinding[A, T]]
) {
  private[configuration] def _encoded_value: Consequence[ConfigurationValue] =
    parameter.codec.encode(value)
}

object ConfigurationBinding {
  def initial[A, T](
    candidate: ConfigurationBindingCandidate[A, T]
  ): Consequence[ConfigurationBinding[A, T]] =
    if (candidate == null)
      Consequence.configurationInvalid("configuration binding candidate is required")
    else
      Consequence.success(
        new ConfigurationBinding(
          candidate.parameter,
          candidate.target,
          candidate.value,
          candidate.provenance,
          None
        )
      )

  def overrideWith[A, T](
    candidate: ConfigurationBindingCandidate[A, T],
    previous: ConfigurationBinding[A, T]
  ): Consequence[ConfigurationBinding[A, T]] =
    if (candidate == null || previous == null)
      Consequence.configurationInvalid("configuration binding override requires candidate and previous binding")
    else if (!(candidate.parameter eq previous.parameter))
      Consequence.configurationInvalid("configuration binding override requires the same parameter witness")
    else if (candidate.parameter.id != previous.parameter.id)
      Consequence.configurationInvalid("configuration binding override requires the same canonical parameter id")
    else
      Consequence.success(
        new ConfigurationBinding(
          candidate.parameter,
          candidate.target,
          candidate.value,
          candidate.provenance,
          Some(previous)
        )
      )
}

final class ConfigurationBindingCandidates[T] private (
  val bindings: Vector[ConfigurationBindingCandidate[?, T]]
)

object ConfigurationBindingCandidates {
  private val _empty = new ConfigurationBindingCandidates[Nothing](Vector.empty)

  def empty[T]: ConfigurationBindingCandidates[T] =
    _empty.asInstanceOf[ConfigurationBindingCandidates[T]]

  def apply[T](): ConfigurationBindingCandidates[T] =
    empty

  def from[T](
    bindings: Vector[ConfigurationBindingCandidate[?, T]]
  ): Consequence[ConfigurationBindingCandidates[T]] =
    if (bindings == null || bindings.exists(_ == null))
      Consequence.configurationInvalid("configuration binding candidates are invalid")
    else
      Consequence.success(new ConfigurationBindingCandidates(bindings))
}

final class ConfigurationBindingCollection[T] private (
  private val _bindings: Map[CanonicalParameterId, ConfigurationBinding[?, T]]
) {
  private[configuration] def _bindings_for_trace: Vector[ConfigurationBinding[?, T]] =
    _bindings.values.toVector

  def binding[A](
    parameter: ConfigurationParameter[A]
  ): Consequence[Option[ConfigurationBinding[A, T]]] =
    if (parameter == null)
      Consequence.configurationInvalid("configuration binding lookup parameter is required")
    else
      _bindings.get(parameter.id) match {
        case None => Consequence.success(None)
        case Some(binding) if binding.parameter.asInstanceOf[AnyRef] eq parameter.asInstanceOf[AnyRef] =>
          Consequence.success(Some(binding.asInstanceOf[ConfigurationBinding[A, T]]))
        case Some(_) =>
          Consequence.configurationInvalid("configuration binding lookup requires the original parameter witness")
      }

  def value[A](
    parameter: ConfigurationParameter[A]
  ): Consequence[Option[A]] =
    binding(parameter) match {
      case Consequence.Success(Some(binding)) => Consequence.success(Some(binding.value))
      case Consequence.Success(None) => Consequence.success(None)
      case Consequence.Failure(conclusion) => Consequence.Failure(conclusion)
    }
}

object ConfigurationBindingCollection {
  private val _empty = new ConfigurationBindingCollection[Nothing](Map.empty)

  def empty[T]: ConfigurationBindingCollection[T] =
    _empty.asInstanceOf[ConfigurationBindingCollection[T]]

  def apply[T](): ConfigurationBindingCollection[T] =
    empty

  def from[T](
    bindings: Vector[ConfigurationBinding[?, T]]
  ): Consequence[ConfigurationBindingCollection[T]] =
    if (bindings == null || bindings.exists(_ == null))
      Consequence.configurationInvalid("configuration binding collection is invalid")
    else {
      val ids = bindings.map(_.parameter.id)
      if (ids.distinct.size != ids.size)
        Consequence.configurationInvalid("configuration binding collection has duplicate canonical parameter ids")
      else
        Consequence.success(new ConfigurationBindingCollection(bindings.map(x => x.parameter.id -> x).toMap))
    }
}
