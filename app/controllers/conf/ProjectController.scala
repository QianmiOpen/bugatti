package controllers.conf

import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._
/**
 * 项目管理
 *
 * @author of546
 */
object ProjectController extends Controller {

  val jobNo = "of123"

  implicit val projectWrites = Json.writes[Project]

  val projectForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "templateId" -> number,
      "subTotal" -> ignored(0),
      "lastVersion" -> optional(text),
      "lastUpdated" -> optional(jodaDate),
      "items" -> list(
        mapping(
          "id" -> optional(number),
          "tid" -> optional(number),
          "name" -> nonEmptyText,
          "value" -> optional(text)
        )(Attribute.apply)(Attribute.unapply)
      )
    )(ProjectForm.apply)(ProjectForm.unapply)
  )

  def index(page: Int, pageSize: Int, search: Option[String]) = Action {
    Ok(Json.toJson(ProjectHelper.all(page, pageSize)))
  }

  def count(search: Option[String]) = Action {
    Ok(Json.toJson(ProjectHelper.count))
  }

  def show(id: Int) = Action {
    Ok(Json.toJson(ProjectHelper.findById(id)))
  }

  def save = Action { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      projectForm => {
        ProjectHelper.findByName(projectForm.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> ProjectHelper.create(projectForm, jobNo)))
        }
      }
    )
  }

  def all = Action {
    Ok(Json.toJson(ProjectHelper.all()))
  }

  // 子项目

}
