package org.simplemodeling.model

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.goldenport.datatype.Identifier
import org.goldenport.datatype.Name
import org.goldenport.datatype.ObjectId
import org.simplemodeling.model.value.AuditAttributes
import org.simplemodeling.model.value.ContextualAttributes
import org.simplemodeling.model.value.DescriptiveAttributes
import org.simplemodeling.model.value.LifecycleAttributes
import org.simplemodeling.model.value.MediaAttributes
import org.simplemodeling.model.value.NameAttributes
import org.simplemodeling.model.value.PublicationAttributes
import org.simplemodeling.model.value.ResourceAttributes
import org.simplemodeling.model.value.SecurityAttributes

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
abstract class SimpleObjectCreate {
  // NOTE:
  // Current priority is executable behavior.
  // This create model is kept in the current shape first, and will be
  // refactored toward fuller ValueObject composition in a later phase.
  def name_Attributes: NameAttributes =
    NameAttributes.simple(Name("unknown"))

  def descriptive_Attributes: DescriptiveAttributes =
    DescriptiveAttributes.empty

  def lifecycle_Attributes: LifecycleAttributes =
    LifecycleAttributes(
      createdAt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
      updatedAt = None,
      createdBy = Identifier("system"),
      updatedBy = None,
      postStatus = org.simplemodeling.model.statemachine.PostStatus.default,
      aliveness = org.simplemodeling.model.statemachine.Aliveness.default
    )

  def publication_Attributes: PublicationAttributes =
    PublicationAttributes(
      publishAt = None,
      publicAt = None,
      closeAt = None,
      startAt = None,
      endAt = None
    )

  def security_Attributes: SecurityAttributes = {
    val sid = ObjectId(Identifier("system"))
    SecurityAttributes(
      ownerId = sid,
      groupId = sid,
      rights = SecurityAttributes.Rights(
        owner = SecurityAttributes.Rights.Permissions(read = true, write = true, execute = true),
        group = SecurityAttributes.Rights.Permissions(read = true, write = false, execute = false),
        other = SecurityAttributes.Rights.Permissions(read = true, write = false, execute = false)
      ),
      privilegeId = sid
    )
  }

  def resource_Attributes: ResourceAttributes =
    ResourceAttributes()

  def audit_Attributes: AuditAttributes =
    AuditAttributes()

  def media_Attributes: MediaAttributes =
    MediaAttributes(
      url = None,
      images = Vector.empty,
      audios = Vector.empty,
      videos = Vector.empty,
      atathments = Vector.empty
    )

  def contextual_Attribute: ContextualAttributes =
    ContextualAttributes()
}
