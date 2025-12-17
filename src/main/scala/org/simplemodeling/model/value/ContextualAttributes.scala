package org.simplemodeling.model.value

/*
 * @since   Aug.  1, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class ContextualAttributes(
)

object ContextualAttributes {
  trait Holder {
    protected def contextual_Attribute: ContextualAttributes
  }
}
