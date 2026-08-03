package org.goldenport.configuration

import org.goldenport.Conclusion

/*
 * @since   Aug.  2, 2026
 * @version Aug.  2, 2026
 * @author  ASAMI, Tomoharu
 */
trait ConfigurationBindingScenarioRequest[T] {
  def scenarioId: String
}

sealed trait ConfigurationBindingScenarioReport

object ConfigurationBindingScenarioReport {
  final case class NotImplemented(
    scenarioId: String
  ) extends ConfigurationBindingScenarioReport

  final case class Executed[T](
    scenarioId: String,
    candidates: ConfigurationBindingCandidates[T]
  ) extends ConfigurationBindingScenarioReport

  final case class Rejected(
    scenarioId: String,
    conclusion: Conclusion
  ) extends ConfigurationBindingScenarioReport
}

trait ConfigurationBindingScenarioSpi[T] {
  def evaluate(
    request: ConfigurationBindingScenarioRequest[T]
  ): ConfigurationBindingScenarioReport
}

object ConfigurationBindingScenarioSpi {
  def notImplemented[T]: ConfigurationBindingScenarioSpi[T] =
    new ConfigurationBindingScenarioSpi[T] {
      override def evaluate(
        request: ConfigurationBindingScenarioRequest[T]
      ): ConfigurationBindingScenarioReport =
        ConfigurationBindingScenarioReport.NotImplemented(request.scenarioId)
    }
}
