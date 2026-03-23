package org.simplemodeling.model.value

import java.time.ZonedDateTime
import org.goldenport.datatype.Identifier
import org.simplemodeling.model.statemachine.PostStatus
import org.simplemodeling.model.statemachine.Aliveness

/*
 * @since   Aug.  1, 2025
 *  version Aug.  4, 2025
 *  version Feb. 19, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
case class LifecycleAttributes(
  createdAt: ZonedDateTime,
  updatedAt: Option[ZonedDateTime],
  createdBy: Identifier,
  updatedBy: Option[Identifier],
  postStatus: PostStatus,
  aliveness: Aliveness
)

object LifecycleAttributes {
  trait Holder {
    protected def lifecycle_Attributes: LifecycleAttributes
  }
}
