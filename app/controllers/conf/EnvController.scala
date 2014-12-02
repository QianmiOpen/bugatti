package controllers.conf

import enums.{ModEnum, FuncEnum, LevelEnum}
import exceptions.UniqueNameException
import models.conf._
import play.api.mvc._
import controllers.BaseController
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._

/**
 * 环境管理
 *
 * @author of546
 */
object EnvController extends BaseController {

  implicit val varWrites = Json.writes[Variable]
  implicit val envWrites = Json.writes[Environment]

  def msg(user: String, ip: String, msg: String, data: Environment) =
    Json.obj("mod" -> ModEnum.env.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val envForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText(maxLength = 30),
      "jobNo" -> optional(text(maxLength = 16)),
      "remark" -> optional(text(maxLength = 250)),
      "nfServer" -> optional(text(maxLength = 30)),
      "ipRange" -> optional(nonEmptyText(maxLength = 300)),
      "level" -> enums.form.enum(LevelEnum),
      "scriptVersion" -> nonEmptyText(maxLength = 30)
    )(Environment.apply)(Environment.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.findById(id)))
  }

  def index(page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.all(page, pageSize)))
  }

  def all = Action {
    Ok(Json.toJson(EnvironmentHelper.all()))
  }

  def count = Action {
    Ok(Json.toJson(EnvironmentHelper.count))
  }

  // 根据权限加载环境列表
  def showAuth = AuthAction(FuncEnum.task) { implicit request =>
    val user = request.user
    // 管理员 & 委员长 显示所有环境
    val countSafe = ProjectMemberHelper.count(request.user.jobNo, LevelEnum.safe)
    val seq =
      if (UserHelper.superAdmin_?(request.user) || countSafe > 0) {
        EnvironmentHelper.all()
      }
      else {
        //环境成员
        val envs = EnvironmentMemberHelper.findEnvsByJobNo(user.jobNo)
        //非安全环境
        val unEnvs = EnvironmentHelper.findByUnsafe()
        //merge
        unEnvs ++ envs.filterNot(t => unEnvs.contains(t))
      }
    Ok(Json.toJson(seq))
  }

  def delete(id: Int) = AuthAction(FuncEnum.env) { implicit request =>
    EnvironmentHelper.findById(id) match {
      case Some(env) =>
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除环境", env))
        Ok(Json.toJson(EnvironmentHelper.delete(id)))
      case None => NotFound
    }
  }

  def allScriptVersion = AuthAction(FuncEnum.env) { implicit request =>
    Ok(Json.toJson(ScriptVersionHelper.allName))
  }

  def save = AuthAction(FuncEnum.env) { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      env =>
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增环境", env))
          Ok(Json.toJson(EnvironmentHelper.create(env.copy(jobNo = Some(request.user.jobNo)))))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.env) { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      env =>
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改环境", env))
          Ok(Json.toJson(EnvironmentHelper.update(id, env)))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
    )
  }

  // ----------------------------------------------------------
  // 环境成员
  // ----------------------------------------------------------
  implicit val memberWrites = Json.writes[EnvironmentMember]

  def member(envId: Int, jobNo: String) = Action {
    Ok(Json.toJson(EnvironmentMemberHelper.findEnvId_JobNo(envId, jobNo.toLowerCase)))
  }

  def members(envId: Int) = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentMemberHelper.findByEnvId(envId)))
  }

  def saveMember(envId: Int, jobNo: String) = AuthAction(FuncEnum.env) { implicit request =>
    if (UserHelper.findByJobNo(jobNo) == None) Ok(_None)
    else EnvironmentHelper.findById(envId) match {
      case Some(env) if env.jobNo == Some(request.user.jobNo) || UserHelper.superAdmin_?(request.user) =>
        try {
          val member = EnvironmentMember(None, envId, jobNo.toLowerCase)
          Ok(Json.toJson(EnvironmentMemberHelper.create(member)))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
      case Some(env) if env.jobNo != Some(request.user.jobNo) => Forbidden
      case _ => NotFound
    }
  }

  def deleteMember(envId: Int, memberId: Int) = AuthAction(FuncEnum.env) { implicit request =>
    EnvironmentHelper.findById(envId) match {
      case Some(env) if env.jobNo == Some(request.user.jobNo) || UserHelper.superAdmin_?(request.user) =>
        Ok(Json.toJson(EnvironmentMemberHelper.delete(memberId)))
      case Some(env) if env.jobNo != Some(request.user.jobNo) => Forbidden
      case _ => NotFound
    }
  }

}
