package controllers.conf

import controllers.BaseController
import enums.{RoleEnum, FuncEnum, LevelEnum}
import models.conf._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._
import play.api.mvc.Action

/**
 * 项目管理
 *
 * @author of546
 */
object ProjectController extends BaseController {

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

  def index(my: Boolean, page: Int, pageSize: Int) = AuthAction(FuncEnum.project) { implicit request =>
    val jobNo = if (my) Some(request.user.jobNo) else None
    Ok(Json.toJson(ProjectHelper.all(jobNo, page, pageSize)))
  }

  def count(my: Boolean) = AuthAction(FuncEnum.project) { implicit request =>
    val jobNo = if (my) Some(request.user.jobNo) else None
    Ok(Json.toJson(ProjectHelper.count(jobNo)))
  }

  def show(id: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ProjectHelper.findById(id)))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    if (!UserHelper.hasProject(id, request.user)) Forbidden
    else
      ProjectHelper.findById(id) match {
        case Some(project) =>
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

  def save = AuthAction(FuncEnum.project) { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      projectForm => {
        ProjectHelper.findByName(projectForm.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> ProjectHelper.create(projectForm, request.user.jobNo)))
        }
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      projectForm => {
        Ok(Json.obj("r" -> ProjectHelper.update(id, projectForm)))
      }
    )
  }

  def all = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(ProjectHelper.all()))
  }

  def showAuth = AuthAction(FuncEnum.project) { implicit request =>
    Ok(Json.toJson(MemberHelper.findProjectsByJobNo(request.user.jobNo)))
  }

  // ----------------------------------------------------------
  // 项目属性
  // ----------------------------------------------------------
  def atts(pid: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(AttributeHelper.findByPid(pid)))
  }

  // ----------------------------------------------------------
  // 项目成员
  // ----------------------------------------------------------
  implicit val memberWrites = Json.writes[Member]

  def members(pid: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(MemberHelper.findByPid(pid)))
  }

  def saveMember(pid: Int, jobNo: String) = AuthAction(FuncEnum.project) {
    UserHelper.findByJobNo(jobNo) match {
      case Some(_) =>
        Ok(Json.obj("r" -> Json.toJson(MemberHelper.create(Member(None, pid, LevelEnum.unsafe, jobNo)))))
      case _ =>
        Ok(Json.obj("r" -> "none"))
    }
  }

  def updateMember(mid: Int, op: String) = AuthAction(FuncEnum.project) {
    MemberHelper.findById(mid) match {
      case Some(member) =>
        op match {
          case "up" =>
            Ok(Json.obj("r" -> MemberHelper.update(mid, member.copy(level = LevelEnum.safe))))
          case "down" =>
            Ok(Json.obj("r" -> MemberHelper.update(mid, member.copy(level = LevelEnum.unsafe))))
          case "remove" =>
            Ok(Json.obj("r" -> MemberHelper.delete(mid)))
          case _ =>
            BadRequest
        }
      case None =>
        NotFound
    }

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
