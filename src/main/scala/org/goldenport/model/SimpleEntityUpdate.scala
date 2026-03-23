package org.goldenport.model

import org.goldenport.model.datatype.EntityId
import org.simplemodeling.model.directive.Update

/*
 * @since   Mar. 23, 2026
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityUpdate extends SimpleObjectUpdate {
  def id: Update[EntityId] = Update.noop[EntityId]
}
