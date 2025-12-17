package org.simplemodeling.model.value

import org.simplemodeling.datatype.ObjectId
import org.simplemodeling.model.SimpleObject

/*
 * @since   Aug.  2, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class Image(
  id: ObjectId,
  simpleobject: SimpleObjectContent
) extends SimpleObject with SimpleObjectContent.Holder {
  protected def simple_Object_Content = simpleobject
}
