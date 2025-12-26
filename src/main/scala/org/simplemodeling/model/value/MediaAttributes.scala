package org.goldenport.model.value

import java.net.URL

/*
 * @since   Aug.  2, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class MediaAttributes(
  url: Option[URL],
  images: Vector[Image],
  audios: Vector[Audio],
  videos: Vector[Video],
  atathments: Vector[Attachment]
)

object MediaAttributes {
  trait Holder {
    protected def media_Attributes: MediaAttributes
  }
}
