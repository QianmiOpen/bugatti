package controllers.conf

import models.conf.ProjectHelper
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
  implicit val projectTypeWrites = Json.writes[ProjectType]

  val projectForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "typeId" -> number,
      "subTotal" -> ignored(0),
      "lastVersion" -> optional(text),
      "lastUpdated" -> optional(jodaDate)
    )(Project.apply)(Project.unapply)
  )

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

  // 项目所有类型
  def types = Action { implicit request =>
    Ok(Json.toJson(ProjectTypeHelper.all))
  }

  def index(page: Int, pageSize: Int, search: Option[String]) = Action {
    Ok(Json.toJson(ProjectHelper.all(page, pageSize)))
  }

  def count(search: Option[String]) = Action {
    Ok(Json.toJson(ProjectHelper.count))
  }

  // 子项目

}
