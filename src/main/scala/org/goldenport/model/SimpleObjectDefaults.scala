package org.goldenport.model

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.goldenport.datatype.Identifier
import org.goldenport.datatype.Name
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
 * @since   Mar. 23, 2026
 * @version Mar. 23, 2026
 * @author  ASAMI, Tomoharu
 */
trait SimpleObjectDefaults extends SimpleObject {
  protected def name_Attributes: NameAttributes =
    NameAttributes.simple(Name("unknown"))

  protected def descriptive_Attributes: DescriptiveAttributes =
    DescriptiveAttributes.empty

  protected def lifecycle_Attributes: LifecycleAttributes =
    LifecycleAttributes(
      createdAt = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC),
      updatedAt = None,
      createdBy = Identifier("system"),
      updatedBy = None,
      postStatus = org.goldenport.model.statemachine.PostStatus.default,
      aliveness = org.goldenport.model.statemachine.Aliveness.default
    )

  protected def publication_Attributes: PublicationAttributes =
    PublicationAttributes(
      publishAt = None,
      publicAt = None,
      closeAt = None,
      startAt = None,
      endAt = None
    )

  protected def security_Attributes: SecurityAttributes = {
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

  protected def resource_Attributes: ResourceAttributes =
    ResourceAttributes()

  protected def audit_Attributes: AuditAttributes =
    AuditAttributes()

  protected def media_Attributes: MediaAttributes =
    MediaAttributes(
      url = None,
      images = Vector.empty,
      audios = Vector.empty,
      videos = Vector.empty,
      atathments = Vector.empty
    )

  protected def contextual_Attribute: ContextualAttributes =
    ContextualAttributes()
}
