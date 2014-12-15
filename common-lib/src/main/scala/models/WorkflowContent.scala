package models

import models.Status._
import com.gu.workflow.db.Schema
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._

case class WorkflowContent(
                            composerId: String,
                            path: Option[String],
                            headline: Option[String],
                            mainMedia: Option[String],
                            contentType: String,
                            section: Option[Section],
                            status: Status,
                            lastModified: DateTime,
                            lastModifiedBy: Option[String],
                            commentable: Boolean,
                            published: Boolean,
                            timePublished: Option[DateTime],
                            storyBundleId: Option[String],
                            activeInInCopy: Boolean,
                            takenDown: Boolean,
                            timeTakenDown: Option[DateTime]
                            ) {

  def updateWith(wireStatus: WireStatus): WorkflowContent =
    copy(
      section = wireStatus.tagSections.headOption,
      status = if (wireStatus.published) Final else status,
      lastModified =  wireStatus.lastModified,
      lastModifiedBy = wireStatus.user,
      published = wireStatus.published
    )
}

object WorkflowContent {

  implicit val dateTimeFormat = DateFormat

  def fromWireStatus(wireStatus: WireStatus, stub: Stub): WorkflowContent = {
    WorkflowContent(
      wireStatus.composerId,
      wireStatus.path,
      wireStatus.headline,
      wireStatus.mainMedia,
      wireStatus.`type`,
      wireStatus.tagSections.headOption,
      wireStatus.status, // not written to the database but the DTO requires a value.
      wireStatus.lastModified,
      wireStatus.user,
      commentable=wireStatus.commentable,
      published = wireStatus.published,
      timePublished = wireStatus.publicationDate,
      storyBundleId = wireStatus.storyBundleId,
      false, // assume not active in incopy
      takenDown = false,
      timeTakenDown = None
    )
  }

  def default(composerId: String, contentType: String = "article", activeInInCopy: Boolean = false): WorkflowContent = {
    WorkflowContent(composerId,
      path=None,
      headline=None,
      mainMedia=None,
      contentType=contentType,
      section=None,
      status=Status.Writers,
      lastModified=new DateTime,
      lastModifiedBy=None,
      commentable=false,
      published=false,
      timePublished=None,
      storyBundleId=None,
      activeInInCopy=activeInInCopy,
      takenDown=false,
      timeTakenDown=None)
  }

  def fromContentRow(row: Schema.ContentRow): WorkflowContent = row match {
    case (composerId, path, lastMod, lastModBy, status, contentType, commentable,
          headline, mainMedia, published, timePublished, _, storyBundleId, activeInInCopy,
          takenDown, timeTakenDown) =>
          WorkflowContent(
            composerId, path, headline, mainMedia, contentType, None, Status(status), lastMod,
            lastModBy, commentable, published, timePublished, storyBundleId,
            activeInInCopy, takenDown, timeTakenDown)
  }
  def newContentRow(wc: WorkflowContent, revision: Option[Long]): Schema.ContentRow =
    (wc.composerId, wc.path, wc.lastModified, wc.lastModifiedBy, wc.status.name,
     wc.contentType, wc.commentable, wc.headline, wc.mainMedia, wc.published, wc.timePublished,
     revision, wc.storyBundleId, wc.activeInInCopy, false, None)

  implicit val workFlowContentWrites: Writes[WorkflowContent] = Json.writes[WorkflowContent]

  implicit val workFlowContentReads: Reads[WorkflowContent] =
    ((__ \ "composerId").read[String] ~
      (__ \ "path").readNullable[String] ~
      (__ \ "headline").readNullable[String] ~
      (__ \ "mainMedia").readNullable[String] ~
      (__ \ "contentType").read[String] ~
      (__ \ "section" \ "name").readNullable[String].map { _.map(s => Section(s))} ~
      (__ \ "status").read[String].map { s => Status(s) } ~
      (__ \ "lastModified").read[DateTime] ~
      (__ \ "lastModifiedBy").readNullable[String] ~
      (__ \ "commentable").read[Boolean] ~
      (__ \ "published").read[Boolean] ~
      (__ \ "timePublished").readNullable[DateTime] ~
      (__ \ "storyBundleId").readNullable[String] ~
      (__ \ "activeInInCopy").read[Boolean] ~
      (__ \ "takenDown").read[Boolean] ~
      (__ \ "timeTakenDown").readNullable[DateTime]
      )(WorkflowContent.apply _)
}

case class ContentItem(stub: Stub, wcOpt: Option[WorkflowContent])

case object ContentItem {
  implicit val contentItemReads = new Reads[ContentItem] {
    def reads(json: JsValue) = {
      for {
        stub <- json.validate[Stub]
        wcOpt <- json.validate[Option[WorkflowContent]]
      } yield ContentItem(stub, wcOpt)
    }
  }
}

