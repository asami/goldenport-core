package org.goldenport.model

import org.goldenport.model.datatype.EntityId
import org.simplemodeling.model.directive.Condition

/*
 * @since   Mar. 23, 2026
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntityQuery extends SimpleObjectQuery {
  def id: Condition[EntityId] = Condition.any[EntityId]
}
