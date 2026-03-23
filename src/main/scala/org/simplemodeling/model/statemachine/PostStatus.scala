package org.simplemodeling.model.statemachine

import org.goldenport.util.SmEnumClass

/*
 * @since   Aug.  2, 2025
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
enum PostStatus(
  val dbValue: Int
) extends StateMachine {
  case Draft extends PostStatus(1)
  case Published extends PostStatus(2)
  case Archived extends PostStatus(3)

  def isPublic: Boolean =
    this == PostStatus.Published
}

object PostStatus extends SmEnumClass[PostStatus] {
  protected def enum_Values = values

  val default: PostStatus = Draft

  object Event {
    val Publish = "publish"
    val Archive = "archive"
    val Restore = "restore"
  }

  private val _by_db_value = values.map(x => x.dbValue -> x).toMap

  def fromDbValue(dbvalue: Int): Option[PostStatus] =
    _by_db_value.get(dbvalue)

  private val _state_machine =
    StateMachineDef(
      name = "post_status",
      states = Vector(
        StateDef(
          Draft.stateName,
          transitions = Vector(
            TransitionDef(Published.stateName, Event.Publish, None, Vector.empty, priority = 10),
            TransitionDef(Archived.stateName, Event.Archive, None, Vector.empty, priority = 20)
          )
        ),
        StateDef(
          Published.stateName,
          transitions = Vector(
            TransitionDef(Archived.stateName, Event.Archive, None, Vector.empty, priority = 10)
          )
        ),
        StateDef(
          Archived.stateName,
          transitions = Vector(
            TransitionDef(Draft.stateName, Event.Restore, None, Vector.empty, priority = 10)
          )
        )
      ),
      initialState = default.stateName
    )

  private val _evaluator = new GuardEvaluator[PostStatus, EventDef] {
    def eval(
      guard: GuardExpr,
      state: PostStatus,
      event: EventDef
    ): Either[GuardError, Boolean] = Right(true)
  }

  def asStateMachineDef: StateMachineDef =
    _state_machine

  def transition(
    current: PostStatus,
    eventname: String
  ): Either[PlannerError, Option[PostStatus]] =
    DeterministicPlanner
      .select(
        sm = _state_machine,
        currentstate = current.stateName,
        eventname = eventname,
        state = current,
        event = EventDef(eventname),
        evaluator = _evaluator
      )
      .map(_.flatMap(x => from(x.transition.to)))
}
