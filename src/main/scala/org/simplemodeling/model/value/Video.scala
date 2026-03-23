package org.simplemodeling.model.value

import org.goldenport.datatype.ObjectId
import org.simplemodeling.model.SimpleObject

/*
 * @since   Aug.  2, 2025
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
case class Video(
  id: ObjectId,
  simpleobject: SimpleObjectContent
) extends SimpleObject with SimpleObjectContent.Holder {
  protected def simple_Object_Content = simpleobject
}
