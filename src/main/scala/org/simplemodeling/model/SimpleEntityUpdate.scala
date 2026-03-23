package org.simplemodeling.model

import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.model.directive.Update

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityUpdate extends SimpleObjectUpdate {
  def id: Update[EntityId] = Update.noop[EntityId]
}
