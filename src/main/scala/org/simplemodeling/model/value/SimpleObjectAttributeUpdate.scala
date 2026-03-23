package org.simplemodeling.model.value

import java.net.URL
import java.time.ZonedDateTime
import cats.data.NonEmptyVector
import org.goldenport.datatype.Identifier
import org.goldenport.datatype.I18nBrief
import org.goldenport.datatype.I18nDescription
import org.goldenport.datatype.I18nLabel
import org.goldenport.datatype.I18nSummary
import org.goldenport.datatype.I18nText
import org.goldenport.datatype.I18nTitle
import org.goldenport.datatype.Name
import org.goldenport.datatype.ObjectId
import org.goldenport.datatype.Slug
import org.simplemodeling.model.statemachine.Aliveness
import org.simplemodeling.model.statemachine.PostStatus
import org.simplemodeling.model.directive.Update

/*
 * @since   Mar. 23, 2026
 * @version Mar. 24, 2026
 * @author  ASAMI, Tomoharu
 */
// NOTE:
// Temporary model to complete the current style first with execution
// prioritized.
// In a later phase, the structure will be reorganized toward better
// alignment with SimpleObject through ValueObject composition.
case class NameAttributesUpdate(
  name: Update[Name] = Update.noop[Name],
  label: Update[I18nLabel] = Update.noop[I18nLabel],
  title: Update[I18nTitle] = Update.noop[I18nTitle],
  code: Update[Identifier] = Update.noop[Identifier],
  alias: Update[NonEmptyVector[I18nLabel]] = Update.noop[NonEmptyVector[I18nLabel]],
  slug: Update[Slug] = Update.noop[Slug],
  shortid: Update[Identifier] = Update.noop[Identifier]
)

case class DescriptiveAttributesUpdate(
  headline: Update[I18nBrief] = Update.noop[I18nBrief],
  brief: Update[I18nBrief] = Update.noop[I18nBrief],
  summary: Update[I18nSummary] = Update.noop[I18nSummary],
  description: Update[I18nDescription] = Update.noop[I18nDescription],
  lead: Update[I18nSummary] = Update.noop[I18nSummary],
  content: Update[I18nText] = Update.noop[I18nText],
  `abstract`: Update[I18nSummary] = Update.noop[I18nSummary],
  remarks: Update[I18nSummary] = Update.noop[I18nSummary],
  tooltip: Update[I18nLabel] = Update.noop[I18nLabel]
)

case class LifecycleAttributesUpdate(
  createdAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  updatedAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  createdBy: Update[Identifier] = Update.noop[Identifier],
  updatedBy: Update[Identifier] = Update.noop[Identifier],
  postStatus: Update[PostStatus] = Update.noop[PostStatus],
  aliveness: Update[Aliveness] = Update.noop[Aliveness]
)

case class PublicationAttributesUpdate(
  publishAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  publicAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  closeAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  startAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime],
  endAt: Update[ZonedDateTime] = Update.noop[ZonedDateTime]
)

case class SecurityAttributesUpdate(
  ownerId: Update[ObjectId] = Update.noop[ObjectId],
  groupId: Update[ObjectId] = Update.noop[ObjectId],
  rights: Update[SecurityAttributes.Rights] = Update.noop[SecurityAttributes.Rights],
  privilegeId: Update[ObjectId] = Update.noop[ObjectId]
)

case class ResourceAttributesUpdate(
)

case class AuditAttributesUpdate(
)

case class MediaAttributesUpdate(
  url: Update[URL] = Update.noop[URL],
  images: Update[Vector[Image]] = Update.noop[Vector[Image]],
  audios: Update[Vector[Audio]] = Update.noop[Vector[Audio]],
  videos: Update[Vector[Video]] = Update.noop[Vector[Video]],
  atathments: Update[Vector[Attachment]] = Update.noop[Vector[Attachment]]
)

case class ContextualAttributesUpdate(
)
