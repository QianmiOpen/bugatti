package controllers.conf

import models.conf._
import org.joda.time.DateTime
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
  implicit val attributeWrites = Json.writes[Attribute]

  val projectForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "templateId" -> number,
      "subTotal" -> default(number, 0),
      "lastVid" -> optional(number),
      "lastVersion" -> optional(text),
      "lastUpdated" -> optional(jodaDate("yyyy-MM-dd hh:mm:ss")),
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

  def index(my: Boolean, page: Int, pageSize: Int) = Action {
    val jobNo = if (my) Some("of546") else None
    Ok(Json.toJson(ProjectHelper.all(jobNo, page, pageSize)))
  }

  def count(my: Boolean) = Action {
    val jobNo = if (my) Some("of546") else None
    Ok(Json.toJson(ProjectHelper.count(jobNo)))
  }

  def show(id: Int) = Action {
    Ok(Json.toJson(ProjectHelper.findById(id)))
  }

  def delete(id: Int) = Action {
    ProjectHelper.findById(id) match {
      case Some(project) =>
        // todo permission
        project.subTotal match {
          case 0 =>
            Ok(Json.obj("r" -> Json.toJson(ProjectHelper.delete(id))))
          case _ =>
            Ok(Json.obj("r" -> "exist"))
        }
      case None =>
        Ok(Json.obj("r" -> "none"))
    }

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

  def update(id: Int) = Action { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      projectForm => {
        Ok(Json.obj("r" -> ProjectHelper.update(id, projectForm)))
      }
    )
  }

  def all = Action {
    Ok(Json.toJson(ProjectHelper.all()))
  }

  // 项目属性
  def atts(pid: Int) = Action {
    Ok(Json.toJson(AttributeHelper.findByPid(pid)))
  }

  // ==========================================================
  // open api
  // ==========================================================
  case class VerForm(projectName: String, groupId: String, artifactId: String, version: String, authToken: String)
  val verForm = Form(
    mapping(
      "projectName" -> nonEmptyText(maxLength = 50),
      "groupId" -> nonEmptyText(maxLength = 50),
      "artifactId" -> nonEmptyText(maxLength = 50),
      "version" -> nonEmptyText(maxLength = 50),
      "authToken" -> nonEmptyText(maxLength = 50)
    )(VerForm.apply)(VerForm.unapply)
  )

  // todo
  implicit val app: play.api.Application = play.api.Play.current
  lazy val authToken = app.configuration.getString("auth.token").getOrElse("bugatti")

  def addVersion() = Action { implicit request =>
    verForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      verData => {
        verData.authToken match {
          case token if token == authToken =>
            ProjectHelper.findByName(verData.projectName) match {
              case Some(project) =>
                VersionHelper.findByPid_Vs(project.id.get, verData.version) match {
                  case Some(_) =>
                    Ok(Json.obj("r" -> "exist"))
                  case None =>
                    VersionHelper.create(Version(None, project.id.get, verData.version, DateTime.now()))
                    Ok(Json.obj("r" -> "ok"))
                }
              case None =>
                NotFound
            }
          case _ =>
            Forbidden
        }
      }
    )
  }

}
