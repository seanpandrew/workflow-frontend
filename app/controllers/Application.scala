package controllers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import lib.Database
import models._
import play.api.data.Form
import java.util.UUID


object Application extends Controller {

  import play.api.data.Forms._

  val workFlowForm = Form(
  mapping(
    "title" -> text,
    "desk" -> text,
    "status" -> text
  )((title, desk, status)=>
        WorkflowContent(UUID.randomUUID(),
        path=None,
        workingTitle=Some(title),
        contributors=Nil,
        desk=Some(EditorDesk(desk)),
        status=WorkflowStatus.findWorkFlowStatus(status).getOrElse(WorkflowStatus.Created),
        lastModification=None,
        scheduledLaunch=None,
        stateHistory=Map.empty,
        fromFeed=false
     ))((w: WorkflowContent) => Some("tmp","tmp", "tmp"))
  )

  def index = Action {
    Ok(views.html.index("Hello wor... kflow :)"))
  }

  def content = Action.async {
    Database.store.future.map(items => {
      val workFlowContent = items.values.toList
      Ok(views.html.contentDashboard(workFlowContent, workFlowForm))
    })
  }

  def newWorkFlow = Action.async { implicit request =>
    workFlowForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest("that failed"))
      },
      contentItem => {
        Database.store.alter(items => items.updated(contentItem.id, contentItem)).map { _ =>
          Redirect(routes.Application.content)
        }
      }
    )
  }

  def fieldChange(field: String, value: String, contentId: String, user: Option[String]) = Action.async {

    val updateFunction: Either[SimpleResult, WorkflowContent => WorkflowContent] = field match {

      case "desk" => Right(_.copy(desk=Some(EditorDesk(value))))

      case "workingTitle" => Right(_.copy(workingTitle = Some(value)))

      case "status" => for { u<-user.toRight(BadRequest("user name not supplied")).right
                             s <- WorkflowStatus.findWorkFlowStatus(value).toRight(BadRequest(s"not a valid status $value")).right
                           } yield (wc: WorkflowContent) => wc.copy(status=s, stateHistory = wc.stateHistory.updated(s,u))


      case "launch" => Formatting.parseDate(value)
                       .map(d => (wc: WorkflowContent) => wc.copy(scheduledLaunch = Some(d)))
                       .toRight(BadRequest(s"not a valid date $value"))

      case f => Left(BadRequest(s"field '$f' doesn't exist"))
    }
    val id: Either[SimpleResult, UUID] = try { Right(UUID.fromString(contentId)) } catch {
      case e: IllegalArgumentException => Left(BadRequest(s"invalid UUID $contentId"))
    }
    (for {
      contentId <- id.right
      fun       <- updateFunction.right
    }
    yield alterContent(contentId, field, fun)).left.map(Future.successful).merge
  }

  def alterContent(contentId: UUID, field: String, fun: WorkflowContent => WorkflowContent): Future[SimpleResult] =
    for (altered <- Database.update(contentId, fun))
    yield altered.map(_ => Ok(s"Updated field $field")).getOrElse(NotFound("Could not find that content.") )

}
