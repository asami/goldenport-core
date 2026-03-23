package org.simplemodeling.model.value

/*
 * @since   Aug.  1, 2025
 *  version Aug.  2, 2025
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
case class AuditAttributes(
)

object AuditAttributes {
  trait Holder {
    protected def audit_Attributes: AuditAttributes
  }
}
