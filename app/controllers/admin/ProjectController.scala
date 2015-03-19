package controllers.admin

import controllers.BaseController
import enums._
import exceptions.UniqueNameException
import models.conf._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc.Action
import utils.Directory._
import utils.{JGitUtil, LockUtil}

/**
 * 项目管理
 *
 * @author of546
 */
object ProjectController extends BaseController {

  implicit val varWrites = Json.writes[Variable]
  implicit val attributeWrites = Json.writes[Attribute]
  implicit val projectWrites = Json.writes[Project]
  implicit val relWrites = Json.writes[Host]

  def msg(user: String, ip: String, msg: String, data: Project) =
    Json.obj("mod" -> ModEnum.project.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  def msg_task(user: String, ip: String, msg: String, data: Host) =
    Json.obj("mod" -> ModEnum.task.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val projectForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "description" -> optional(text),
      "templateId" -> number,
      "subTotal" -> default(number, 0),
      "lastVid" -> optional(number),
      "lastVersion" -> optional(text),
      "lastUpdated" -> optional(jodaDate("yyyy-MM-dd HH:mm:ss")),
      "items" -> seq(
        mapping(
          "id" -> optional(number),
          "projectId" -> optional(number),
          "name" -> nonEmptyText,
          "value" -> optional(text)
        )(Attribute.apply)(Attribute.unapply)
      ),
      "variables" -> seq(
        mapping(
          "id" -> optional(number),
          "envId" -> optional(number),
          "projectId" -> optional(number),
          "name" -> nonEmptyText,
          "value" -> nonEmptyText
        )(Variable.apply)(Variable.unapply)
      )
    )(ProjectForm.apply)(ProjectForm.unapply)
  )

  def index(projectName: Option[String], page: Int, pageSize: Int) = AuthAction() { implicit request =>
    Ok(Json.toJson(ProjectHelper.all(projectName.filterNot(_.isEmpty), page, pageSize)))
  }

  def count(projectName: Option[String]) = AuthAction() { implicit request =>
    Ok(Json.toJson(ProjectHelper.count(projectName.filterNot(_.isEmpty))))
  }

  def show(id: Int) = Action {
    Ok(Json.toJson(ProjectHelper.findById(id)))
  }

  def all = Action {
    Ok(Json.toJson(ProjectHelper.all()))
  }

  def my(jobNo: String) = Action {
    Ok(Json.toJson(ProjectMemberHelper.findProjectsByJobNo(jobNo)))
  }

  def allExceptSelf(id: Int) = Action {
    Ok(Json.toJson(ProjectHelper.allExceptSelf(id)))
  }

  // 根据权限加载不同环境的项目列表
  def showAuth(envId: Int) = AuthAction() { implicit request =>
    if (UserHelper.hasEnv(envId, request.user)) {
      Ok(Json.toJson(ProjectHelper.all()))
    } else {
      EnvironmentHelper.findById(envId) match {
        case Some(e) if e.level == LevelEnum.safe =>
          Ok(Json.toJson(ProjectMemberHelper.findSafeProjectsByJobNo(request.user.jobNo)))
        case _ =>
          Ok(Json.toJson(ProjectMemberHelper.findProjectsByJobNo(request.user.jobNo)))
      }
    }
  }

  def delete(id: Int) = AuthAction() { implicit request =>
    if (!UserHelper.hasProjectSafe(id, request.user)) Forbidden
    else ProjectHelper.findById(id) match {
      case Some(project) => project.subTotal match {
        case 0 =>
          // Remove repositories
          org.apache.commons.io.FileUtils.deleteDirectory(getRepositoryDir(id))

          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除项目", project))
          Ok(Json.toJson(ProjectHelper.delete(id)))
        case _ => Ok(_Exist)
      }
      case None => NotFound
    }
  }

  def save = AuthAction() { implicit request =>
    def createRepository(projectId: Int) = {
      LockUtil.lock(s"${projectId}") {
        val gitDir = getRepositoryDir(projectId)
        try {
          JGitUtil.initRepository(gitDir)
        } catch {
          case ie: IllegalStateException => play.api.Logger.warn("Create Repository Error:", ie)
        }
      }
    }
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      projectForm => {
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增项目", projectForm.toProject))
        try {
          val projectId = ProjectHelper.create(projectForm, request.user.jobNo)
          createRepository(projectId) // Create the actual repository
          Ok(Json.toJson(projectId))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
      }
    )
  }

  def update(projectId: Int, envId: Int) = AuthAction() { implicit request =>
    projectForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      projectForm => {
        if (UserHelper.hasProjectSafe(projectId, request.user) ||
            UserHelper.hasEnv(envId, request.user)
        ) {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改项目", projectForm.toProject))
          try {
            Ok(Json.toJson(ProjectHelper.update(projectId, envId, projectForm)))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        } else Forbidden
      }
    )
  }

  // ----------------------------------------------------------
  // 项目属性
  // ----------------------------------------------------------
  def atts(projectId: Int) = Action {
    Ok(Json.toJson(AttributeHelper.findByProjectId(projectId)))
  }

  // ----------------------------------------------------------
  // 项目环境变量
  // ----------------------------------------------------------
  def vars(projectId: Int, envId: Int) = Action {
    Ok(Json.toJson(VariableHelper.findByEnvId_ProjectId(envId, projectId)))
  }

  // ----------------------------------------------------------
  // 项目成员
  // ----------------------------------------------------------
  implicit val memberWrites = Json.writes[ProjectMember]

  def msg(user: String, ip: String, msg: String, data: ProjectMember) =
    Json.obj("mod" -> ModEnum.member.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  def member(projectId: Int, jobNo: String) = Action {
    Ok(Json.toJson(ProjectMemberHelper.findByProjectId_JobNo(projectId, jobNo.toLowerCase)))
  }

  def members(projectId: Int) = AuthAction() {
    Ok(Json.toJson(ProjectMemberHelper.findByProjectId(projectId)))
  }

  def saveMember(projectId: Int, jobNo: String) = AuthAction() { implicit request =>
    if (UserHelper.findByJobNo(jobNo) == None) Ok(_None)
    else if (!UserHelper.hasProjectSafe(projectId, request.user)) Forbidden
    else {
      try {
        val member = ProjectMember(None, projectId, LevelEnum.unsafe, jobNo.toLowerCase)
        val mid = ProjectMemberHelper.create(member)
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增项目成员", member.copy(Some(mid))))
        Ok(Json.toJson(mid))
      } catch {
        case un: UniqueNameException => Ok(_Exist)
      }
    }
  }

  def updateMember(memberId: Int, op: String) = AuthAction() { implicit request =>
    ProjectMemberHelper.findById(memberId) match {
      case Some(member) =>
        if (!UserHelper.hasProjectSafe(member.projectId, request.user)) Forbidden
        else op match {
          case "up" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "升级项目成员", member))
            Ok(Json.toJson(ProjectMemberHelper.update(memberId, member.copy(level = LevelEnum.safe))))
          case "down" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "降级项目成员", member))
            Ok(Json.toJson(ProjectMemberHelper.update(memberId, member.copy(level = LevelEnum.unsafe))))
          case "remove" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "剔除项目成员", member))
            Ok(Json.toJson(ProjectMemberHelper.delete(memberId)))
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
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      verData => verData.authToken match {
        case token if token == authToken =>
          ProjectHelper.findByName(verData.projectName) match {
            case Some(project) =>
              VersionHelper.findByProjectId_Vs(project.id.get, verData.version) match {
                case Some(_) => Conflict(_Exist)
                case None =>
                  VersionHelper.create(Version(None, project.id.get, verData.version, DateTime.now()))
                  Ok(_Success)
              }
            case None => NotFound
          }
        case _ => Unauthorized
      }
    )
  }

  case class RelForm(projectId: Int, envId: Int, areaId: Int, ip: Option[String])
  val relForm = Form(
    mapping(
      "projectId" -> number,
      "envId" -> number,
      "areaId" -> number,
      "ip" -> optional(text)
    )(RelForm.apply)(RelForm.unapply)
  )

  def addCluster = AuthAction() { implicit request =>
    relForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      rel => {
        val rels = HostHelper.findByEnvId_AreaId(rel.envId, rel.areaId)
        val unbind = rels.filter(_.projectId.isEmpty)
        val bind = rels.filter(_.projectId.isDefined)

        if (unbind.size < 1) {
          Ok(_None) // 没有可用机器资源
        } else rel.ip match {
          case ip if ip.isEmpty || ip.get.trim.isEmpty =>
            val prel = HostHelper.findByEnvId_ProjectId(rel.envId, rel.projectId)
            val prelHosts = prel.groupBy(_.hostIp).map(_._1).toSet
            val fn = unbind.filterNot(ub => prelHosts.contains(ub.hostIp))
            if (fn.isEmpty) {
              Ok(_None.+("u", Json.toJson("host")))
            } else {
              val hostIp = fn.groupBy(_.hostIp).mapValues(_.size).toSeq.sortBy(_._2).last._1
              val update2rel = fn.filter(_.hostIp == hostIp).head.copy(projectId = Some(rel.projectId))
              val result = HostHelper.update(update2rel)
              ALogger.info(msg_task(request.user.jobNo, request.remoteAddress, s"增加机器:$result", update2rel))
              Ok(_Success)
            }
          case Some(ip) if bind.exists(_.ip == ip) => Ok(_Exist)
          case Some(ip) if unbind.exists(_.ip == ip) =>
            val update2rel = unbind.find(_.ip == ip).head.copy(projectId = Some(rel.projectId))
            HostHelper.update(update2rel)
            ALogger.info(msg_task(request.user.jobNo, request.remoteAddress, "增加机器", update2rel))
            Ok(_Success)
          case _ => Ok(_Fail)
        }
      }
    )
  }

  def removeCluster(clusterId: Int) = AuthAction() { implicit request =>
    HostHelper.findById(clusterId) match {
      case Some(rel) =>
        val result = HostHelper.unbind(rel)
        ALogger.info(msg_task(request.user.jobNo, request.remoteAddress, "移除机器", rel))
        Ok(Json.obj("r" -> result))
      case _ => Ok(Json.obj("r" -> 0))
    }
  }

}
