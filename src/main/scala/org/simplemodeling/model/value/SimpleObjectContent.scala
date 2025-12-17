package org.simplemodeling.model.value

/*
 * @since   Aug.  1, 2025
 * @version Aug.  2, 2025
 * @author  ASAMI, Tomoharu
 */
case class SimpleObjectContent(
  nameAttributes: NameAttributes,
  descriptiveAttributes: DescriptiveAttributes,
  lifecycleAttributes: LifecycleAttributes,
  publicationAttributes: PublicationAttributes,
  securityAttributes: SecurityAttributes,
  resourceAttributes: ResourceAttributes,
  auditAttributes: AuditAttributes,
  mediaAttributes: MediaAttributes,
  contextualAttribute: ContextualAttributes
) {
}

object SimpleObjectContent {
  trait Holder {
    protected def simple_Object_Content: SimpleObjectContent

    protected def name_Attributes: NameAttributes = simple_Object_Content.nameAttributes
    protected def descriptive_Attributes: DescriptiveAttributes = simple_Object_Content.descriptiveAttributes
    protected def lifecycle_Attributes: LifecycleAttributes = simple_Object_Content.lifecycleAttributes
    protected def publication_Attributes: PublicationAttributes = simple_Object_Content.publicationAttributes
    protected def security_Attributes: SecurityAttributes = simple_Object_Content.securityAttributes
    protected def resource_Attributes: ResourceAttributes = simple_Object_Content.resourceAttributes
    protected def audit_Attributes: AuditAttributes = simple_Object_Content.auditAttributes
    protected def media_Attributes: MediaAttributes = simple_Object_Content.mediaAttributes
    protected def contextual_Attribute: ContextualAttributes = simple_Object_Content.contextualAttribute
  }
}
