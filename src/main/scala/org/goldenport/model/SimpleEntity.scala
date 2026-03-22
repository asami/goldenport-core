package org.goldenport.model

import org.goldenport.model.datatype.EntityId

/*
 * @since   Aug.  4, 2025
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntity extends SimpleObjectDefaults {
  def id: EntityId
}
