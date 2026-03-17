package org.goldenport.model.statemachine

import org.goldenport.util.SmEnumClass

/*
 * @since   Aug.  2, 2025
 * @version Mar. 18, 2026
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

  private val _by_db_value = values.map(x => x.dbValue -> x).toMap

  def fromDbValue(dbvalue: Int): Option[PostStatus] =
    _by_db_value.get(dbvalue)
}
