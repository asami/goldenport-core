package org.simplemodeling.model

import org.simplemodeling.model.datatype.EntityId
import org.simplemodeling.model.directive.Condition

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityQuery extends SimpleObjectQuery {
  def id: Condition[EntityId] = Condition.any[EntityId]
}
