package org.goldenport.provisional.conclusion

import org.goldenport.provisional.observation.Observation

case class Conclusion(
  observation: Observation,
  interpretation: Interpretation,
  disposition: Disposition,
  previous: Option[Conclusion] = None
)

case class Interpretation(
  kind: Interpretation.Kind,
  responsibility: Disposition.Responsibility // TODO
)
object Interpretation {
  enum Kind(val name: String, val value: Int) {
    case DomainFailure extends Kind("domain-failure", 1)
    case Defect extends Kind("defect", 2)
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
}
