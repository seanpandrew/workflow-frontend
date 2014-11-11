package controllers

import com.gu.workflow.db.{DeskDB, SectionDB}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import play.api.mvc._
import play.api.data.Form

import lib._
import models.{Status => WorkflowStatus, Section, Desk}

object Admin extends Controller with PanDomainAuthActions {

  import play.api.data.Forms._

  def index(selectedDeskIdOption: Option[Long]) = AuthAction {

    val deskList = DeskDB.deskList

    val selectedDeskOption = for {
      selectedDeskId <- selectedDeskIdOption
      selectedDesk <- deskList.find((desk) => selectedDeskId == desk.id)
    } yield {
      selectedDesk
    }

    selectedDeskOption match {
      case Some(selectedDesk) => { // If we have a selected desk then show the desk as selected and the related sections

        val mappedDesks = deskList.map((desk) => { // We want the selected desk to show as selected in the list of desks
          if (desk.id == selectedDesk.id) {
            desk.copy(name = desk.name, selected = true)
          } else {
            desk
          }
        })

        Ok(
          views.html.adminConsole(
            DeskDB.getSectionsWithRelation(selectedDesk).sortBy(_.name),
            addSectionForm,
            mappedDesks.sortBy(_.name),
            addDeskForm,
            Some(selectedDesk))
        )
      }
      case None => Ok( // No desk, just show list of desks and sections
        views.html.adminConsole(
          SectionDB.sectionList.sortBy(_.name),
          addSectionForm,
          deskList.sortBy(_.name),
          addDeskForm,
          None)
      )
    }

  }

  val addSectionForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "selected" -> boolean,
      "id" -> longNumber
    )(Section.apply)(Section.unapply)
  )

  val addDeskForm = Form(
    mapping(
      "name" -> nonEmptyText,
      "selected" -> boolean,
      "id" -> longNumber
    )(Desk.apply)(Desk.unapply)
  )

  case class assignSectionToDeskFormData(desk: Long, sections: List[String])

  val assignSectionToDeskForm = Form(
    mapping(
      "desk" -> longNumber,
      "sections" -> list(text)
    )(assignSectionToDeskFormData.apply)(assignSectionToDeskFormData.unapply)
  )

  def assignSectionToDesk = AuthAction { implicit request =>
    assignSectionToDeskForm.bindFromRequest.fold(
      formWithErrors => BadRequest("failed to update section assignments"),
      sectionAssignment => {
        DeskDB.assignSectionsToDesk(sectionAssignment.desk, sectionAssignment.sections.map(id => id.toLong))
        Redirect(routes.Admin.index(Some(sectionAssignment.desk)))
      }
    )
  }

  /*
    SECTION routes
   */

  def addSection = AuthAction { implicit request =>
    addSectionForm.bindFromRequest.fold(
      formWithErrors => BadRequest("failed to add section"),
      section => {
        SectionDB.upsert(section)
        Redirect(routes.Admin.index(None))
      }
    )
  }

  def removeSection = AuthAction { implicit request =>
    addSectionForm.bindFromRequest.fold(
      formWithErrors => BadRequest("failed to remove section"),
      section => {
        SectionDB.remove(section)
        NoContent
      }
    )
  }

  /*
    DESK routes
   */

  def addDesk = AuthAction { implicit request =>
    addDeskForm.bindFromRequest.fold(
      formWithErrors => BadRequest(s"failed to add desk ${formWithErrors.errors}"),
      desk => {
        DeskDB.upsert(desk)
        Redirect(routes.Admin.index(None))
      }
    )
  }

  def removeDesk = AuthAction { implicit request =>
    addDeskForm.bindFromRequest.fold(
      formWithErrors => BadRequest("failed to remove desk"),
      desk => {
        DeskDB.remove(desk)
        NoContent
      }
    )
  }

  val statusForm = Form(
    mapping(
      "name" -> nonEmptyText
    )(WorkflowStatus.apply)(WorkflowStatus.unapply)
  )

  def status = AuthAction.async {
    for (statuses <- StatusDatabase.statuses) yield Ok(views.html.status(statuses, statusForm))
  }

  def addStatus = processStatusUpdate("failed to add status") { status =>
    StatusDatabase.add(status).map{ _ =>
      Redirect(routes.Admin.status)
    }
  }

  def removeStatus = processStatusUpdate("failed to remove status") { status =>
    StatusDatabase.remove(status).map{ _ =>
      Redirect(routes.Admin.status)
    }
  }

  def moveStatusUp = processStatusUpdate("failed to move status") { status =>
    StatusDatabase.moveUp(status).map{ _ =>
      Redirect(routes.Admin.status)
    }
  }

  def moveStatusDown = processStatusUpdate("failed to move status") { status =>
    StatusDatabase.moveDown(status).map{ _ =>
      Redirect(routes.Admin.status)
    }
  }

  def processStatusUpdate(error: String)(block: WorkflowStatus => Future[Result]) = Action.async { implicit request =>
    statusForm.bindFromRequest.fold(
      formWithErrors => {
        Future.successful(BadRequest(error))
      },
      block
    )
  }
}
