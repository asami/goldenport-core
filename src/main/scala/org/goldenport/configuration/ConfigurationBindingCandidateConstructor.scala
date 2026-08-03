package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
sealed trait ConfigurationBindingCandidateInput[T] {
  private[configuration] def construct(
    snapshot: ConfigurationSourceSnapshot[?]
  ): Consequence[ConfigurationBindingCandidate[?, T]]
}

object ConfigurationBindingCandidateInput {
  final class Typed[A, T] private[configuration] (
    val parameter: ConfigurationParameter[A],
    val target: T,
    val rawValue: ConfigurationValue,
    val inputPath: Option[String],
    val inputSpelling: Option[String],
    val evidence: Vector[String],
    val isConfidential: Boolean
  ) extends ConfigurationBindingCandidateInput[T] {
    override private[configuration] def construct(
      snapshot: ConfigurationSourceSnapshot[?]
    ): Consequence[ConfigurationBindingCandidate[?, T]] =
      for {
        provenance <- ConfigurationProvenance.create(
          snapshot.origin,
          snapshot.layer,
          snapshot.sourceIdentity,
          inputPath,
          inputSpelling,
          snapshot.sourceRank,
          snapshot.sourceOrdinal,
          evidence,
          isConfidential,
          snapshot.sourceType
        )
        value <- parameter.codec.decode(rawValue)
        candidate <- ConfigurationBindingCandidate.create(parameter, target, value, provenance)
      } yield candidate
  }

  def typed[A, T](
    parameter: ConfigurationParameter[A],
    target: T,
    rawValue: ConfigurationValue,
    inputPath: Option[String],
    inputSpelling: Option[String],
    evidence: Vector[String],
    isConfidential: Boolean
  ): Consequence[ConfigurationBindingCandidateInput[T]] =
    if (parameter == null || target == null || rawValue == null || inputPath == null ||
      inputSpelling == null || evidence == null)
      Consequence.configurationInvalid("configuration binding candidate input is invalid")
    else
      Consequence.success(
        new Typed(
          parameter,
          target,
          rawValue,
          inputPath,
          inputSpelling,
          evidence,
          isConfidential
        )
      )
}

final class ConfigurationBindingCandidateBatch[T] private (
  val snapshot: ConfigurationSourceSnapshot[?],
  val inputs: Vector[ConfigurationBindingCandidateInput[T]]
)

object ConfigurationBindingCandidateBatch {
  def create[T](
    snapshot: ConfigurationSourceSnapshot[?],
    inputs: Vector[ConfigurationBindingCandidateInput[T]]
  ): Consequence[ConfigurationBindingCandidateBatch[T]] =
    if (snapshot == null || inputs == null || inputs.exists(_ == null))
      Consequence.configurationInvalid("configuration binding candidate batch is invalid")
    else
      Consequence.success(new ConfigurationBindingCandidateBatch(snapshot, inputs))
}

object ConfigurationBindingCandidateConstructor {
  def construct[T](
    batches: Vector[ConfigurationBindingCandidateBatch[T]]
  ): Consequence[ConfigurationBindingCandidates[T]] =
    if (batches == null || batches.exists(_ == null))
      Consequence.configurationInvalid("configuration binding candidate batches are invalid")
    else
      _construct(batches).flatMap { candidates =>
        val identities = candidates.map(x => (x._1.collisionDomain, x._2.parameter.id, x._2.target))
        if (identities.distinct.size != identities.size)
          Consequence.configurationInvalid("configuration binding candidates contain a duplicate canonical parameter and target")
        else
          ConfigurationBindingCandidates.from(candidates.map(_._2))
      }

  private def _construct[T](
    batches: Vector[ConfigurationBindingCandidateBatch[T]]
  ): Consequence[Vector[(ConfigurationSourceSnapshot[?], ConfigurationBindingCandidate[?, T])]] =
    batches.foldLeft(Consequence.success(Vector.empty[(ConfigurationSourceSnapshot[?], ConfigurationBindingCandidate[?, T])])) {
      case (acc, batch) =>
        batch.inputs.foldLeft(acc) { (candidates, input) =>
          for {
            values <- candidates
            candidate <- input.construct(batch.snapshot)
          } yield values :+ (batch.snapshot -> candidate)
        }
    }
}
