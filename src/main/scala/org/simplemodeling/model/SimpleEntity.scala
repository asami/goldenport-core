package org.simplemodeling.model

import org.simplemodeling.model.datatype.EntityId

/*
 * @since   Aug.  4, 2025
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntity extends SimpleObjectDefaults {
  def id: EntityId

  // Exposes title as a first-class SimpleEntity attribute for model-generated entities.
  def title: String =
    name_Attributes.title.map(_.value.displayMessage).getOrElse("")
}
