package org.goldenport.model

import org.goldenport.datatype.ObjectId
import org.goldenport.model.value.NameAttributes
import org.goldenport.model.value.DescriptiveAttributes
import org.goldenport.model.value.LifecycleAttributes
import org.goldenport.model.value.PublicationAttributes
import org.goldenport.model.value.SecurityAttributes
import org.goldenport.model.value.ResourceAttributes
import org.goldenport.model.value.AuditAttributes
import org.goldenport.model.value.MediaAttributes
import org.goldenport.model.value.ContextualAttributes

/*
 * @since   Aug.  4, 2025
 * @version Aug.  4, 2025
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleEntity extends SimpleObject {
  def id: ObjectId
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
