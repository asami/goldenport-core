package org.goldenport.model.value

import java.time.ZonedDateTime
import org.goldenport.datatype.EntityReference
import org.goldenport.model.statemachine.PostStatus
import org.goldenport.model.statemachine.Aliveness

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
