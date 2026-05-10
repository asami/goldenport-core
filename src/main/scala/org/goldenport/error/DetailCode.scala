package org.goldenport.error

import org.goldenport.Conclusion
import org.goldenport.observation.Cause
import org.goldenport.record.Record

/**
 * Numeric semantic error detail code.
 *
 * DetailCode is a compact, machine-readable classification derived from the
 * structured Conclusion model. It is not a protocol status code and must not be
 * used as a CLI exit code.
 */
/*
 * @since   May. 11, 2026
 * @version May. 11, 2026
 * @author  ASAMI, Tomoharu
 */
case class DetailCode(
  code: Long
) extends AnyVal {
  def toRecord: Record = Record.data("code" -> code)
}

object DetailCode {
  final case class Dimensions(
    category: Int,
    symptom: Int,
    cause: Int,
    interpretation: Int,
    userAction: Int,
    responsibility: Int
  ) {
    def code: Long =
      category * 10000000000L +
        symptom * 100000000L +
        cause * 1000000L +
        interpretation * 10000L +
        userAction * 100L +
        responsibility

    def toDetailCode: DetailCode = DetailCode(code)

    def toRecord: Record = Record.data(
      "category" -> category,
      "symptom" -> symptom,
      "cause" -> cause,
      "interpretation" -> interpretation,
      "userAction" -> userAction,
      "responsibility" -> responsibility
    )
  }

  def generated(conclusion: Conclusion): DetailCode =
    dimensions(conclusion).toDetailCode

  def dimensions(conclusion: Conclusion): Dimensions = {
    val taxonomy = conclusion.observation.taxonomy
    val cause = conclusion.observation.cause.kind.getOrElse(Cause.Kind.Unknown)
    Dimensions(
      category = taxonomy.category.value,
      symptom = taxonomy.symptom.value,
      cause = cause.value,
      interpretation = conclusion.interpretation.kind.value,
      userAction = conclusion.disposition.userAction.map(_.value).getOrElse(0),
      responsibility = conclusion.disposition.responsibility.map(_.value).getOrElse(0)
    )
  }

  def path(conclusion: Conclusion): Record = {
    val taxonomy = conclusion.observation.taxonomy
    val cause = conclusion.observation.cause.kind.getOrElse(Cause.Kind.Unknown)
    Record.data(
      "category" -> taxonomy.category.name,
      "symptom" -> taxonomy.symptom.name,
      "cause" -> cause.name,
      "interpretation" -> conclusion.interpretation.kind.name
    ) ++ Record.dataOption(
      "userAction" -> conclusion.disposition.userAction.map(_.name),
      "responsibility" -> conclusion.disposition.responsibility.map(_.name)
    )
  }

}
