package org.simplemodeling.model

import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityCreate extends SimpleObjectCreate {
  def id: Option[EntityId] = None
}
