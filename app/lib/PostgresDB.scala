package lib

import java.sql.SQLException

import com.wordnik.swagger.annotations.ApiResponses
import lib.OrderingImplicits._
import Response.Response
import models.Flag.Flag
import models._
import com.github.tototoshi.slick.PostgresJodaSupport._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{JsObject, Writes}
import scala.slick.collection.heterogenous._
import syntax._
import scala.slick.driver.PostgresDriver.simple._
import com.gu.workflow.db.Schema._
import com.gu.workflow.syntax._
import com.gu.workflow.db.CommonDB._
import com.gu.workflow.query._

object PostgresDB {

  import play.api.Play.current
  import play.api.db.slick.DB

  def getContent(q: WfQuery): List[DashboardRow] = {
    getContentDBRes(q).map { case (stubData, contentData) =>
      val stub = Stub.fromStubRow(stubData)
      val content = WorkflowContent.fromContentRow(contentData)
      DashboardRow(stub, content)
    }
  }

  def getContentDBRes(q: WfQuery) = {
    DB.withTransaction { implicit session =>
      WfQuery.getContentQuery(q).list
    }
  }

  def contentItemLookup(composerId: String): List[DashboardRow] =
    DB.withTransaction { implicit session =>
      WfQuery.contentLookup(composerId)
        .filter( {case (s,c) => ContentItem.visibleOnUi(s, c) })
        .list.map { case (stubData, contentData) =>
        val stub    = Stub.fromStubRow(stubData)
        val content = WorkflowContent.fromContentRow(contentData)
        DashboardRow(stub, content)
      }
    }

  def getContentItems(q: WfQuery): List[ContentItem] = {
    val dbRes = getContentItemsDBRes(q)
    val content: List[ContentItem] = dbRes.map { case (s, c) => {
      ContentItem(Stub.fromStubRow(s), WorkflowContent.fromOptionalContentRow(c))
    }}
    content
  }

  def getContentItemsDBRes(q: WfQuery) = {
    DB.withTransaction { implicit session =>
      val leftJoinQ = (for {
        (s, c) <- (WfQuery.stubsQuery(q) leftJoin WfQuery.contentQuery(q) on (_.composerId === _.composerId))
      } yield (s,  c.?))
      leftJoinQ
    }
  }


  /**
   * Creates a new content item in Workflow.
   *
   * @param stub
   * @param contentItem
   * @return Either: Left(Long) if item exists already with composerId.
   *         Right(Long) of newly created item.
   */

  /* TODO :- Return Option of Long with another function for the API response */

  def createContent(contentItem: ContentItem): Response[Long] = {
    DB.withTransaction { implicit session =>

      val existing = contentItem.wcOpt.flatMap(wc => (for (s <- stubs if s.composerId === wc.composerId) yield s.pk).firstOption)

      existing match {
        case Some(stubId) => Left(ApiErrors.conflict)
        case None => {
          val stubId = ((stubs returning stubs.map(_.pk)) += Stub.newStubRow(contentItem.stub))
          contentItem.wcOpt.foreach(content += WorkflowContent.newContentRow(_, None))

          Right(ApiSuccess(stubId))
        }
      }
    }
  }

  def getContentById(id: Long): Option[ContentItem] = {
    DB.withTransaction { implicit session =>
      (for {
        (s, c)<- stubs leftJoin content on (_.composerId === _.composerId)
        if s.pk === id
      } yield (s,  c.?)).firstOption.map { case (s, c) => {
        ContentItem(Stub.fromStubRow(s), WorkflowContent.fromOptionalContentRow(c))
      }}
    }
  }

  def getContentByCompserId(composerId: String): Option[ContentItem] = {
    DB.withTransaction { implicit session =>
      (for {
        (s, c)<- stubs leftJoin content on (_.composerId === _.composerId)
        if s.composerId === composerId
      } yield (s,  c.?)).firstOption.map { case (s, c) => {
        ContentItem(Stub.fromStubRow(s), WorkflowContent.fromOptionalContentRow(c))
      }}
    }
  }

  def getDashboardRowByComposerId(composerId: String): Response[DashboardRow] = {
    DB.withTransaction { implicit session =>

      val query = for {
        s <- stubs.filter(_.composerId === composerId)
        c <- content
        if s.composerId === c.composerId
      } yield (s, c)

      val dashboardRowOpt = query.firstOption map {case (stubData, contentData) =>
        val stub    = Stub.fromStubRow(stubData)
        val content = WorkflowContent.fromContentRow(contentData).copy(
          section = Some(Section(stub.section))
        )
        DashboardRow(stub, content)
      }
      dashboardRowOpt match {
        case None => Left(ApiErrors.composerIdNotFound(composerId))
        case Some(d) => Right(ApiSuccess(d))
      }
    }
  }

  def updateContentItem(id: Long, c: ContentItem): Response[Long] = {
    DB.withTransaction { implicit session =>
      val existingContentItem: Option[(Long, Option[String])] = (for {
        (s, c) <- (stubs leftJoin content on (_.composerId === _.composerId))
        if (s.pk === id)
      } yield (s.pk, c.composerId.?)).firstOption

      existingContentItem.map(cItem => {
        cItem match {
          case (sId, Some(composerId)) => Left(ApiErrors.composerItemLinked(sId, composerId))
          case (sId, None) => {
            val stub = c.stub
            try {
              val updatedRow = stubs
                .filter(_.pk === id)
                .map(s => (s.workingTitle, s.section, s.due, s.assignee, s.assigneeEmail, s.composerId, s.contentType, s.priority, s.prodOffice, s.needsLegal, s.note))
                .update((stub.title, stub.section, stub.due, stub.assignee, stub.assigneeEmail, stub.composerId, stub.contentType, stub.priority, stub.prodOffice, stub.needsLegal, stub.note))
              if (updatedRow == 0) Left(ApiErrors.updateError(id))
              else {
                c.wcOpt.foreach(wc => content += WorkflowContent.newContentRow(wc, None))
                Right(ApiSuccess(id))
              }
            }
            catch {
              case sqle: SQLException=> {
                Logger.error(s"Error updating stub with id ${id}, ${sqle.getMessage()}")
                Left(ApiErrors.databaseError(sqle.getMessage()))
              }
            }
          }
        }
      }).getOrElse(Left(ApiErrors.updateError(id)))
    }
  }

  def updateStubWithAssignee(id: Long, assignee: Option[String]): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.assignee)
        .update(assignee)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubWithAssigneeEmail(id: Long, assigneeEmail: Option[String]): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.assigneeEmail)
        .update(assigneeEmail)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubDueDate(id: Long, dueDate: Option[DateTime]): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.due)
        .update(dueDate)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubNote(id: Long, input: String): Response[Long] = {
    val note: Option[String] = if(input.length > 0) Some(input) else None
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.note)
        .update(note)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubProdOffice(id: Long, prodOffice: String): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.prodOffice)
        .update(prodOffice)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubSection(id: Long, section: String): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.section)
        .update(section)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubWorkingTitle(id: Long, workingTitle: String): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.workingTitle)
        .update(workingTitle)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubPriority(id: Long, priority: Int): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.priority)
        .update(priority)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def updateStubTrashed(id: Long, trashed: Option[Boolean]): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.trashed)
        .update(trashed)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }

  }

  def updateStubLegalStatus(id: Long, status: Flag): Response[Long] = {
    DB.withTransaction { implicit session =>
      val updatedRow = stubs
        .filter(_.pk === id)
        .map(s => s.needsLegal)
        .update(status)
      if(updatedRow==0) Left(ApiErrors.updateError(id))
      else Right(ApiSuccess(id))
    }
  }

  def stubLinkedToComposer(id: Long): Boolean = {
    DB.withTransaction { implicit session =>
      val q = stubs.filter(stub => stub.pk === id && stub.composerId.isDefined)
      q.length.run > 0
    }
  }
  //todo - rename to delete contentitem
  def deleteStub(id: Long): Response[Long] = {
    DB.withTransaction { implicit session =>

      archiveContentQuery((s, c) => s.pk === id)

      val queryCurrentStub = stubs.filter(_.pk === id)

      // Delete from Content table
      content.filter(c => c.composerId in queryCurrentStub.map(_.composerId)).delete

      // Delete from Stub table
      val deleted = queryCurrentStub.delete
      if(deleted == 0) Right(ApiSuccess(id)) else Left(ApiErrors.notFound)
    }
  }

  def updateContentStatus(status: String, composerId: String): Response[String] = {
    DB.withTransaction { implicit session =>
      val q = for {
        wc <- content if wc.composerId === composerId
      } yield wc.status
      val updatedRow = q.update(status)
      if(updatedRow==0) Left(ApiErrors.composerIdNotFound(composerId))
      else {
        stubs.filter(_.composerId === composerId).map(_.lastModified).update(DateTime.now())
        Right(ApiSuccess(composerId))
      }
    }
  }
}
