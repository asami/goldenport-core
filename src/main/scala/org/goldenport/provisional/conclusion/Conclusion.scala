package org.goldenport.provisional.conclusion

import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Cause

/*
 * @since   Jan. 25, 2026
 * @version Jan. 29, 2026
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition,
  previous: Option[Conclusion] = None
)

case class Interpretation(
  kind: Interpretation.Kind,
  responsibility: Option[Disposition.Responsibility] = None // TODO
)
object Interpretation {
  enum Kind(val name: String, val value: Int) {
    case Success extends Kind("success", 3)
    case DomainFailure extends Kind("domain-failure", 1)
    case SystemFailure extends Kind("system-failure", 4)
    case NetworkFailure extends Kind("network-failure", 5)
    case ExternalServiceFailure extends Kind("external-service-failure", 6)
    case Defect extends Kind("defect", 2)
  }

  val success = Interpretation(Kind.Success)
  val defect = Interpretation(Kind.Defect, Some(Disposition.Responsibility.Developer))
  val domainFailure = Interpretation(Kind.DomainFailure)
  val systemFailure = Interpretation(Kind.SystemFailure, Some(Disposition.Responsibility.SystemAdmin))
  val networkFailure = Interpretation(Kind.NetworkFailure, Some(Disposition.Responsibility.SystemAdmin))
  val externalServiceFailure = Interpretation(Kind.ExternalServiceFailure, Some(Disposition.Responsibility.SystemAdmin))

  def argumentMissing = domainFailure
  def argumentRedundant = domainFailure
  def argumentDataType = domainFailure
  def argumentConstraint = domainFailure
  def operationInvalid = domainFailure
  def valueInvalid = domainFailure
  def valueFormatError = domainFailure

  def from(e: Throwable): Interpretation = e match {
    case _ => systemFailure // TODO
  }
}

case class Disposition(
  userAction: Option[Disposition.UserAction],
  responsibility: Option[Disposition.Responsibility]
)
object Disposition {
  enum UserAction(val name: String, val value: Int) {
    case FixInput extends UserAction("fix-input", 1)
    case RetryNow extends UserAction("retry-now", 2)
    case RetryLater extends UserAction("retry-later", 3)
    case Escalation extends UserAction("escalation", 4)
  }

  enum Responsibility(val name: String, val value: Int) {
    case User extends Responsibility("user", 1)
    case ApplicationAdmin extends Responsibility("application-admin", 2)
    case SystemAdmin extends Responsibility("system-admin", 3)
    case Developer extends Responsibility("developer", 4)
  }

  val none = Disposition(None, None)
  val fix = Disposition(Some(UserAction.FixInput), None)
  val defect = Disposition(Some(UserAction.Escalation), Some(Responsibility.Developer))

  def argumentMissing = fix
  def argumentRedundant = fix
  def argumentDataType = fix
  def argumentConstraint = fix
  def operationInvalid = fix
  def valueInvalid = fix
  def valueFormatError = fix

  def from(e: Throwable): Disposition = e match {
    case _ => none
  }
}
