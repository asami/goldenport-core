package org.goldenport.model.value

import org.goldenport.datatype.ObjectId
import org.goldenport.model.SimpleObject

/*
 * @since   Aug.  2, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class Video(
  id: ObjectId,
  simpleobject: SimpleObjectContent
) extends SimpleObject with SimpleObjectContent.Holder {
  protected def simple_Object_Content = simpleobject
}
