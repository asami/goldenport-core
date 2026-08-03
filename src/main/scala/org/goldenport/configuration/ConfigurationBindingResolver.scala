package org.goldenport.configuration

import org.goldenport.Consequence

/*
 * @since   Aug.  3, 2026
 * @version Aug.  3, 2026
 * @author  ASAMI, Tomoharu
 */
final class ConfigurationBindingResolutionContext[T] private (
  val targets: Vector[T]
) {
  def specificityOf(target: T): Option[Int] = {
    val index = targets.indexOf(target)
    if (index < 0) None else Some(index)
  }
}

object ConfigurationBindingResolutionContext {
  def create[T](
    targets: Vector[T]
  ): Consequence[ConfigurationBindingResolutionContext[T]] =
    if (targets == null || targets.isEmpty || targets.exists(_ == null) || targets.distinct.size != targets.size)
      Consequence.configurationInvalid("configuration binding resolution targets are invalid")
    else
      Consequence.success(new ConfigurationBindingResolutionContext(targets))
}

object ConfigurationBindingResolver {
  private final case class CandidateScore[T](
    candidate: ConfigurationBindingCandidate[?, T],
    specificity: Int
  )

  def resolve[T](
    candidates: ConfigurationBindingCandidates[T],
    context: ConfigurationBindingResolutionContext[T]
  ): Consequence[ConfigurationBindingCollection[T]] =
    if (candidates == null || context == null)
      Consequence.configurationInvalid("configuration binding resolution input is invalid")
    else {
      val eligible = candidates.bindings.flatMap { candidate =>
        context.specificityOf(candidate.target).map(CandidateScore(candidate, _))
      }
      eligible.groupBy(_.candidate.parameter.id).values.toVector.foldLeft(
        Consequence.success(Vector.empty[ConfigurationBinding[?, T]])
      ) { (acc, group) =>
        for {
          bindings <- acc
          binding <- _resolve_parameter(group)
        } yield bindings :+ binding
      }.flatMap(ConfigurationBindingCollection.from)
    }

  private def _resolve_parameter[T](
    candidates: Vector[CandidateScore[T]]
  ): Consequence[ConfigurationBinding[?, T]] = {
    _validate_parameter_witnesses(candidates).flatMap { _ =>
      val winners = candidates.groupBy { score =>
        score.candidate.provenance.sourceRank -> score.candidate.provenance.sourceOrdinal
      }.values.toVector
      winners.foldLeft(Consequence.success(Vector.empty[ConfigurationBindingCandidate[?, T]])) {
        (acc, sourcecandidates) =>
          for {
            selected <- acc
            winner <- _source_winner(sourcecandidates)
          } yield selected :+ winner
      }.flatMap { sourcewinners =>
        sourcewinners.sortBy { candidate =>
          candidate.provenance.sourceRank -> candidate.provenance.sourceOrdinal
        }.foldLeft(Consequence.success(Option.empty[ConfigurationBinding[?, T]])) {
          case (acc, candidate) =>
            acc.flatMap {
              case None => ConfigurationBinding.initial(candidate).map(Some(_))
              case Some(previous) => _override(candidate, previous).map(Some(_))
            }
        }.flatMap {
          case Some(binding) => Consequence.success(binding)
          case None => Consequence.configurationInvalid("configuration binding resolution has no eligible candidate")
          }
      }
    }
  }

  private def _validate_parameter_witnesses[T](
    candidates: Vector[CandidateScore[T]]
  ): Consequence[Unit] =
    candidates.headOption match {
      case Some(first) if candidates.forall { candidate =>
        candidate.candidate.parameter.asInstanceOf[AnyRef] eq first.candidate.parameter.asInstanceOf[AnyRef]
      } =>
        Consequence.success(())
      case Some(_) =>
        Consequence.configurationInvalid("configuration binding resolution requires the original parameter witness")
      case None =>
        Consequence.configurationInvalid("configuration binding resolution has no eligible candidate")
    }

  private def _source_winner[T](
    candidates: Vector[CandidateScore[T]]
  ): Consequence[ConfigurationBindingCandidate[?, T]] = {
    val maximum = candidates.map(_.specificity).max
    val winners = candidates.filter(_.specificity == maximum)
    if (winners.size != 1)
      Consequence.configurationInvalid("configuration binding resolution has conflicting same-source candidates")
    else
      Consequence.success(winners.head.candidate)
  }

  private def _override[T](
    candidate: ConfigurationBindingCandidate[?, T],
    previous: ConfigurationBinding[?, T]
  ): Consequence[ConfigurationBinding[?, T]] =
    if (!(candidate.parameter.asInstanceOf[AnyRef] eq previous.parameter.asInstanceOf[AnyRef]))
      Consequence.configurationInvalid("configuration binding resolution requires the original parameter witness")
    else
      ConfigurationBinding.overrideWith(
        candidate.asInstanceOf[ConfigurationBindingCandidate[Any, T]],
        previous.asInstanceOf[ConfigurationBinding[Any, T]]
      ).map(x => x: ConfigurationBinding[?, T])
}
