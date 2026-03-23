package org.goldenport.model

import org.goldenport.model.datatype.EntityId

/*
 * @since   Mar. 23, 2026
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityCreate extends SimpleObjectCreate {
  def id: Option[EntityId] = None
}
