package org.simplemodeling.model

import org.simplemodeling.model.value.AuditAttributesUpdate
import org.simplemodeling.model.value.ContextualAttributesUpdate
import org.simplemodeling.model.value.DescriptiveAttributesUpdate
import org.simplemodeling.model.value.LifecycleAttributesUpdate
import org.simplemodeling.model.value.MediaAttributesUpdate
import org.simplemodeling.model.value.NameAttributesUpdate
import org.simplemodeling.model.value.PublicationAttributesUpdate
import org.simplemodeling.model.value.ResourceAttributesUpdate
import org.simplemodeling.model.value.SecurityAttributesUpdate

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleObjectUpdate {
  // NOTE:
  // Execution is prioritized, so this current style is adopted as the
  // temporary completed form.
  // In a later phase, this will be moved toward a more consistent
  // ValueObject-composition model, aligned with SimpleObject itself.
  def name_Attributes: NameAttributesUpdate =
    NameAttributesUpdate()

  def descriptive_Attributes: DescriptiveAttributesUpdate =
    DescriptiveAttributesUpdate()

  def lifecycle_Attributes: LifecycleAttributesUpdate =
    LifecycleAttributesUpdate()

  def publication_Attributes: PublicationAttributesUpdate =
    PublicationAttributesUpdate()

  def security_Attributes: SecurityAttributesUpdate =
    SecurityAttributesUpdate()

  def resource_Attributes: ResourceAttributesUpdate =
    ResourceAttributesUpdate()

  def audit_Attributes: AuditAttributesUpdate =
    AuditAttributesUpdate()

  def media_Attributes: MediaAttributesUpdate =
    MediaAttributesUpdate()

  def contextual_Attribute: ContextualAttributesUpdate =
    ContextualAttributesUpdate()
}
