package org.goldenport.model

import org.goldenport.model.value.AuditAttributesQuery
import org.goldenport.model.value.ContextualAttributesQuery
import org.goldenport.model.value.DescriptiveAttributesQuery
import org.goldenport.model.value.LifecycleAttributesQuery
import org.goldenport.model.value.MediaAttributesQuery
import org.goldenport.model.value.NameAttributesQuery
import org.goldenport.model.value.PublicationAttributesQuery
import org.goldenport.model.value.ResourceAttributesQuery
import org.goldenport.model.value.SecurityAttributesQuery

/*
 * @since   Mar. 23, 2026
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleObjectQuery {
  // NOTE:
  // This is completed first in the current style to prioritize execution.
  // A future phase will reorganize and integrate it toward the same
  // ValueObject-composition design direction as SimpleObject.
  def name_Attributes: NameAttributesQuery =
    NameAttributesQuery()

  def descriptive_Attributes: DescriptiveAttributesQuery =
    DescriptiveAttributesQuery()

  def lifecycle_Attributes: LifecycleAttributesQuery =
    LifecycleAttributesQuery()

  def publication_Attributes: PublicationAttributesQuery =
    PublicationAttributesQuery()

  def security_Attributes: SecurityAttributesQuery =
    SecurityAttributesQuery()

  def resource_Attributes: ResourceAttributesQuery =
    ResourceAttributesQuery()

  def audit_Attributes: AuditAttributesQuery =
    AuditAttributesQuery()

  def media_Attributes: MediaAttributesQuery =
    MediaAttributesQuery()

  def contextual_Attribute: ContextualAttributesQuery =
    ContextualAttributesQuery()
}
