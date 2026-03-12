package org.goldenport.provisional.conclusion

import org.goldenport.record.Record
import org.goldenport.provisional.observation.Observation
import org.goldenport.provisional.observation.Cause

/*
 * @since   Jan. 25, 2026
 *  version Jan. 31, 2026
 *  version Feb.  7, 2026
 * @version Mar. 11, 2026
 * @author  ASAMI, Tomoharu
 */
case class Conclusion(
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition,
  previous: Option[Conclusion] = None
) {
  def withPrevious(p: Conclusion) = copy(previous = Some(p))
}

case class Interpretation(
  kind: Interpretation.Kind,
  responsibility: Option[Disposition.Responsibility] = None // TODO unused
) {
  def toRecord: Record = Record.dataAuto(
    "kind" -> kind.name,
    "responsibility" -> responsibility.map(_.name)
  )
}
object Interpretation {
  enum Kind(val name: String, val value: Int) {
    case Success extends Kind("success", 3)
    case DomainFailure extends Kind("domain-failure", 1)
    case SystemFailure extends Kind("system-failure", 4)
    case NetworkFailure extends Kind("network-failure", 5)
    case ExternalServiceFailure extends Kind("external-service-failure", 6)
    case Defect extends Kind("defect", 2)
    case ConfigurationFailure extends Kind("configuration-failure", 7)
  }

  val success = Interpretation(Kind.Success)
  val defect = Interpretation(Kind.Defect, Some(Disposition.Responsibility.Developer))
  val domainFailure = Interpretation(Kind.DomainFailure)
  val systemFailure = Interpretation(Kind.SystemFailure, Some(Disposition.Responsibility.SystemAdmin))
  val networkFailure = Interpretation(Kind.NetworkFailure, Some(Disposition.Responsibility.SystemAdmin))
  val externalServiceFailure = Interpretation(Kind.ExternalServiceFailure, Some(Disposition.Responsibility.SystemAdmin))
  val configurationFailure = Interpretation(Kind.ConfigurationFailure)

  def argumentMissing = domainFailure
  def argumentRedundant = domainFailure
  def argumentDataType = domainFailure
  def argumentConstraint = domainFailure
  def operationInvalid = domainFailure
  def resourceInconsistency = domainFailure
  def recordNotFound = domainFailure
  def operationNotFound = domainFailure
  def valueInvalid = domainFailure
  def valueFormatError = domainFailure
  def resourceNotFound = domainFailure
  def unreachableReached = defect
  def uninitializedState = defect
  def impossibleState = defect
  def unsupported = defect
  def notImplemented = defect
  def invariantViolation = defect
  def preconditionViolation = defect
  def postconditionViolation = defect

  def from(e: Throwable): Interpretation = e match {
    case _ => systemFailure // TODO
  }
}

case class Disposition(
  userAction: Option[Disposition.UserAction],
  responsibility: Option[Disposition.Responsibility]
) {
  def toRecord: Record = Record.dataOption(
    "user-action" -> userAction.map(_.name),
    "responsibility" -> responsibility.map(_.name)
  )
}
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
  val fix = Disposition(UserAction.FixInput)
  val serviceUnavailable = Disposition(UserAction.Escalation, Responsibility.SystemAdmin)
  val defect = Disposition(UserAction.Escalation, Responsibility.Developer)

  def argumentMissing = fix
  def argumentRedundant = fix
  def argumentDataType = fix
  def argumentConstraint = fix
  def operationInvalid = fix
  def resourceInconsistency = fix
  def recordNotFound = fix
  def operationNotFound = fix
  def valueInvalid = fix
  def valueFormatError = fix
  def unreachableReached = defect
  def uninitializedState = defect
  def impossibleState = defect
  def unsupported = defect
  def notImplemented = defect
  def invariantViolation = defect
  def preconditionViolation = defect
  def postconditionViolation = defect

  def apply(u: UserAction): Disposition = Disposition(Some(u), None)
  def apply(u: UserAction, r: Responsibility): Disposition =
    Disposition(Some(u), Some(r))

  def from(e: Throwable): Disposition = e match {
    case _ => none
  }
}
