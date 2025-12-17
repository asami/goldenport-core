package org.simplemodeling.model.value

import java.time.ZonedDateTime
import org.simplemodeling.datatype.EntityReference
import org.simplemodeling.model.statemachine.PostStatus
import org.simplemodeling.model.statemachine.Aliveness

/*
 * @since   Aug.  1, 2025
 * @version Aug.  4, 2025
 * @author  ASAMI, Tomoharu
 */
case class LifecycleAttributes(
  createdAt: ZonedDateTime,
  updatedAt: Option[ZonedDateTime],
  createdBy: EntityReference,
  updatedBy: Option[EntityReference],
  postStatus: PostStatus,
  aliveness: Aliveness
)

object LifecycleAttributes {
  trait Holder {
    protected def lifecycle_Attributes: LifecycleAttributes
  }
}
