package org.simplemodeling.model

import org.simplemodeling.datatype.ObjectId
import org.simplemodeling.model.value.NameAttributes
import org.simplemodeling.model.value.DescriptiveAttributes
import org.simplemodeling.model.value.LifecycleAttributes
import org.simplemodeling.model.value.PublicationAttributes
import org.simplemodeling.model.value.SecurityAttributes
import org.simplemodeling.model.value.ResourceAttributes
import org.simplemodeling.model.value.AuditAttributes
import org.simplemodeling.model.value.MediaAttributes
import org.simplemodeling.model.value.ContextualAttributes

/*
 * @since   Aug.  1, 2025
 * @version Aug.  4, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleObject extends NameAttributes.Holder
    with DescriptiveAttributes.Holder
    with LifecycleAttributes.Holder
    with PublicationAttributes.Holder
    with SecurityAttributes.Holder
    with ResourceAttributes.Holder
    with AuditAttributes.Holder
    with MediaAttributes.Holder   
    with ContextualAttributes.Holder {
  // protected def name_Attributes: NameAttributes
  // protected def descriptive_Attributes: DescriptiveAttributes
  // protected def lifecycle_Attributes: LifecycleAttributes
  // protected def publication_Attributes: PublicationAttributes
  // protected def security_Attributes: SecurityAttributes
  // protected def resource_Attributes: ResourceAttributes
  // protected def audit_Attributes: AuditAttributes
  // protected def media_Attributes: MediaAttributes
  // protected def contextual_Attribute: ContextualAttributes
}
