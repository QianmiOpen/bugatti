package controllers.conf

import controllers.BaseController
import enums.{ModEnum, RoleEnum, FuncEnum, LevelEnum}
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

  def msg(user: String, ip: String, msg: String, data: Project) =
    s"mod:${ModEnum.project}|user:${user}|ip:${ip}|msg:${msg}|data:${Json.toJson(data)}"

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
          "projectId" -> optional(number),
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

  def show(id: Int) = Action {
    Ok(Json.toJson(ProjectHelper.findById(id)))
  }

  def all = Action {
    Ok(Json.toJson(ProjectHelper.all()))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    if (!UserHelper.hasProjectSafe(id, request.user)) Forbidden
    else
      ProjectHelper.findById(id) match {
        case Some(project) => project.subTotal match {
          case 0 =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除项目", project))
            Ok(Json.obj("r" -> Json.toJson(ProjectHelper.delete(id))))
          case _ => Ok(Json.obj("r" -> "exist"))
        }
        case None => Ok(Json.obj("r" -> "none"))
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
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增项目", projectForm.toProject))
            Ok(Json.obj("r" -> ProjectHelper.create(projectForm, request.user.jobNo)))
        }
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      projectForm => {
        if (!UserHelper.hasProjectSafe(id, request.user)) Forbidden
        else
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改项目", projectForm.toProject))
          Ok(Json.obj("r" -> ProjectHelper.update(id, projectForm)))
      }
    )
  }

  def showAuth = AuthAction(FuncEnum.project) { implicit request =>
    val user = request.user
    if(user.role == RoleEnum.admin){
      Ok(Json.toJson(ProjectHelper.all()))
    }
    else {
      Ok(Json.toJson(MemberHelper.findProjectsByJobNo(request.user.jobNo)))
    }
  }

  // ----------------------------------------------------------
  // 项目属性
  // ----------------------------------------------------------
  def atts(projectId: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(AttributeHelper.findByProjectId(projectId)))
  }

  // ----------------------------------------------------------
  // 项目成员
  // ----------------------------------------------------------
  implicit val memberWrites = Json.writes[Member]

  def msg(user: String, ip: String, msg: String, data: Member) =
    s"mod:${ModEnum.member}|user:${user}|ip:${ip}|msg:${msg}|data:${Json.toJson(data)}"

  def member(projectId: Int, jobNo: String) = Action {
    Ok(Json.toJson(MemberHelper.findByProjectId_JobNo(projectId, jobNo)))
  }

  def members(projectId: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(MemberHelper.findByProjectId(projectId)))
  }

  def saveMember(projectId: Int, jobNo: String) = AuthAction(FuncEnum.project) { implicit request =>
    if (!UserHelper.hasProjectSafe(projectId, request.user)) Forbidden
    else UserHelper.findByJobNo(jobNo) match {
      case Some(_) =>
        val member = Member(None, projectId, LevelEnum.unsafe, jobNo)
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增成员", member))
        Ok(Json.obj("r" -> Json.toJson(MemberHelper.create(member))))
      case _ =>
        Ok(Json.obj("r" -> "none"))
    }
  }

  def updateMember(memberId: Int, op: String) = AuthAction(FuncEnum.project) { implicit request =>
    MemberHelper.findById(memberId) match {
      case Some(member) =>
        if (!UserHelper.hasProjectSafe(member.projectId, request.user)) Forbidden
        else op match {
          case "up" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "升级成员", member))
            Ok(Json.obj("r" -> MemberHelper.update(memberId, member.copy(level = LevelEnum.safe))))
          case "down" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "降级成员", member))
            Ok(Json.obj("r" -> MemberHelper.update(memberId, member.copy(level = LevelEnum.unsafe))))
          case "remove" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "剔除成员", member))
            Ok(Json.obj("r" -> MemberHelper.delete(memberId)))
          case _ => BadRequest
        }
      case None => NotFound
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

  lazy val authToken = app.configuration.getString("auth.token").getOrElse("bugatti")

  /**
   * OpenApi接口: 添加项目新版本
   * desc: 这里不使用AuthAction，不存在内部登录
   *
   * URL Method: POST
   * FormData:
   *   projectName 项目名称
   *   groupId     组名
   *   artifactId  工件名
   *   version     版本
   *   authToken   开放平台的token值
   *
   * @return 401:token错误,404:项目不存在,200:r->exist已存在版本,200:r->ok添加成功
   */
  def addVersion() = Action { implicit request =>
    verForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      verData => verData.authToken match {
        case token if token == authToken =>
          ProjectHelper.findByName(verData.projectName) match {
            case Some(project) =>
              VersionHelper.findByProjectId_Vs(project.id.get, verData.version) match {
                case Some(_) => Conflict(Json.obj("r" -> "exist"))
                case None =>
                  VersionHelper.create(Version(None, project.id.get, verData.version, DateTime.now()))
                  Ok(Json.obj("r" -> "ok"))
              }
            case None => NotFound
          }
        case _ => Unauthorized
      }
    )
  }

}
