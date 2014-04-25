package models

import play.api.libs.json.{Writes, Json, JsValue, Reads}
import org.joda.time.DateTime
import java.util.UUID
import WorkflowStatus._

case class Contributor(name: String)

object Contributor {
  implicit val contributorReads: Reads[Contributor] = new Reads[Contributor] {
    def reads(jsValue: JsValue) = (jsValue \ "internalName").validate[String].map(Contributor(_))
  }
}

case class EditorDesk(name: String) {
  override def toString = name
}

object EditorDesk {
  implicit val deskReads: Reads[EditorDesk] = new Reads[EditorDesk] {
    def reads(jsValue: JsValue) = (jsValue \ "tag" \ "section" \ "name").validate[String].map(EditorDesk(_))
  }
}

case class WireStatus(
  contributors: List[Contributor],
  path: String,
  published: Boolean,
  whatChanged: String,
  user: Option[String],
  lastModified: DateTime,
  tagSections: List[EditorDesk],
  status: WorkflowStatus)

case class WorkflowContent(
  id: UUID,
  path: Option[String],
  workingTitle: Option[String],
  contributors: List[Contributor],
  desk: Option[EditorDesk],
  status: WorkflowStatus,
  lastModification: Option[ContentModification],
  scheduledLaunch: Option[DateTime],
  stateHistory: Map[WorkflowStatus,String] = Map.empty,
  fromFeed: Boolean) {

  def updateWith(wireStatus: WireStatus): WorkflowContent =
    copy(
      contributors = wireStatus.contributors,
      desk = wireStatus.tagSections.headOption,
      status = if (wireStatus.published) Published else status,
      lastModification = Some(ContentModification(
        whatChanged = wireStatus.whatChanged,
        dateTime = wireStatus.lastModified,
        user = wireStatus.user
      ))
    )

}

object WorkflowContent {
  import java.util.UUID

  def fromWireStatus(wireStatus: WireStatus): WorkflowContent = {
    WorkflowContent(
      UUID.randomUUID(),
      Some(wireStatus.path),
      None,
      wireStatus.contributors,
      wireStatus.tagSections.headOption,
      if (wireStatus.published) Published else Created,
      Some(ContentModification(wireStatus.whatChanged, wireStatus.lastModified, wireStatus.user)),
      scheduledLaunch=None,
      fromFeed=true
    )
  }



}

case class ContentModification(
  whatChanged: String,
  dateTime: DateTime,
  user: Option[String])


import play.api.libs.json._
import play.api.libs.functional.syntax._

object WireStatus {

  val readContributors = new Reads[List[Contributor]] {
    def reads(json: JsValue): JsResult[List[Contributor]] =
      (json \ "content" \ "taxonomy" \ "contributors")
        .validate[Option[List[Contributor]]]
        .map(_.toList.flatten)
  }

  val readTagSections = new Reads[List[EditorDesk]] {
    def reads(json: JsValue): JsResult[List[EditorDesk]] = {
      (json \ "content" \ "taxonomy" \ "tags").validate[Option[List[EditorDesk]]].map(_.toList.flatten)
    }

  }

  def readUser = new Reads[Option[String]] {
    def reads(json: JsValue): JsResult[Option[String]] =
      for {
        firstOpt <- (json \ "content" \ "lastModifiedBy" \ "firstName").validate[Option[String]]
        lastOpt  <- (json \ "content" \ "lastModifiedBy" \ "lastName").validate[Option[String]]
      }
      yield firstOpt.flatMap(f => lastOpt.map(l => f + " " + l))
  }

  import WorkflowStatus._
  implicit val wireStatusReads: Reads[WireStatus] =
    ( readContributors ~
      (__ \ "content" \ "identifiers" \ "path").read[String] ~
      (__ \ "published").read[Boolean] ~
      (__ \ "whatChanged").read[String] ~
      readUser ~
      (__ \ "content" \ "lastModified").read[Long].map(t => new DateTime(t)) ~
      readTagSections ~
      (__ \ "published").read[Boolean].map(p => if (p) Published else Created)
      )(WireStatus.apply _)
}

sealed trait WorkflowStatus

object WorkflowStatus {
  def findWorkFlowStatus(status: String): Option[WorkflowStatus] = {
    status match {
      case "created" => Some(Created)
      case "author" => Some(Author)
      case "edited" => Some(Edited)
      case "desk" => Some(Desk)
      case "subbed" => Some(Subbed)
      case "published" => Some(Published)
      case _ => None
    }
  }

  case object Created   extends WorkflowStatus
  case object Author    extends WorkflowStatus
  case object Desk      extends WorkflowStatus
  case object Subbed    extends WorkflowStatus
  case object Edited    extends WorkflowStatus
  case object Published extends WorkflowStatus
}
