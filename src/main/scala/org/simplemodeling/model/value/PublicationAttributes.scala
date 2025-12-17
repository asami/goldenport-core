package org.simplemodeling.model.value

import java.time.ZonedDateTime

/*
 * @since   Aug.  1, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class PublicationAttributes(
  publishAt: Option[ZonedDateTime],
  publicAt: Option[ZonedDateTime],
  closeAt: Option[ZonedDateTime],
  startAt: Option[ZonedDateTime],
  endAt: Option[ZonedDateTime]
)

object PublicationAttributes {
  trait Holder {
    protected def publication_Attributes: PublicationAttributes
  }
}
