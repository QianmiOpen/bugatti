package controllers.admin

import controllers.BaseController
import enums.{RoleEnum, LevelEnum, ModEnum}
import exceptions.UniqueNameException
import models.conf._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import play.api.mvc._

import scala.collection.immutable.TreeSet

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
      "remark" -> optional(text(maxLength = 250)),
      "nfServer" -> optional(text(maxLength = 30)),
      "ipRange" -> optional(nonEmptyText(maxLength = 300)),
      "level" -> enums.form.enum(LevelEnum),
      "locked" -> boolean,
      "scriptVersion" -> nonEmptyText(maxLength = 30)
    )(Environment.apply)(Environment.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.findById(id)))
  }

  def index(page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.all(page, pageSize)))
  }

  def all = AuthAction() { implicit request =>
    if (UserHelper.admin_?(request.user)) {
      Ok(Json.toJson(EnvironmentHelper.all()))
    } else {
      val lockEnvs = EnvironmentHelper.all().filterNot(_.locked)
      val myEnvs = EnvironmentMemberHelper.findEnvsByJobNo(request.user.jobNo)
      Ok(Json.toJson((lockEnvs ++ myEnvs).toSet.toSeq.sortBy((env: Environment) => env.id).reverse))
    }
  }

  def my(jobNo: String) = Action {
    Ok(Json.toJson(EnvironmentMemberHelper.findEnvsByJobNo(jobNo)))
  }

  def count = Action {
    Ok(Json.toJson(EnvironmentHelper.count))
  }

  def allScriptVersion = AuthAction() { implicit request =>
    Ok(Json.toJson(ScriptVersionHelper.allName))
  }

  def delete(id: Int) = AuthAction() { implicit request =>
    if (!UserHelper.hasEnvSafe(id, request.user)) Forbidden
    else EnvironmentHelper.findById(id) match {
        case Some(env) =>
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除环境", env))
          Ok(Json.toJson(EnvironmentHelper.delete(id)))
        case None => NotFound
    }
  }

  def save = AuthAction() { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      _envForm =>
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增环境", _envForm))
          Ok(Json.toJson(EnvironmentHelper.create(_envForm, request.user.jobNo)))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
    )
  }

  def update(id: Int) = AuthAction() { implicit request =>
    if (!UserHelper.hasEnvSafe(id, request.user)) Forbidden
    else envForm.bindFromRequest.fold(
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

  def msg(user: String, ip: String, msg: String, data: EnvironmentMember) =
    Json.obj("mod" -> ModEnum.member.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  def member(envId: Int, jobNo: String) = Action {
    Ok(Json.toJson(EnvironmentMemberHelper.findByEnvId_JobNo(envId, jobNo.toLowerCase)))
  }

  def members(envId: Int) = AuthAction() {
    Ok(Json.toJson(EnvironmentMemberHelper.findByEnvId(envId)))
  }

  def saveMember(envId: Int, jobNo: String) = AuthAction() { implicit request =>
    if (UserHelper.findByJobNo(jobNo) == None) Ok(_None)
    else if (!UserHelper.hasEnvSafe(envId, request.user)) Forbidden
    else {
      try {
        val member = EnvironmentMember(None, envId, LevelEnum.unsafe, jobNo.toLowerCase)
        val mid = EnvironmentMemberHelper.create(member)
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增环境成员", member.copy(Some(mid))))
        Ok(Json.toJson(mid))
      } catch {
        case un: UniqueNameException => Ok(_Exist)
      }
    }
  }

  def updateMember(memberId: Int, op: String) = AuthAction() { implicit  request =>
    EnvironmentMemberHelper.findById(memberId) match {
      case Some(member) =>
        if (!UserHelper.hasEnvSafe(member.envId, request.user)) Forbidden
        else op match {
          case "up" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "升级环境成员", member))
            Ok(Json.toJson(EnvironmentMemberHelper.update(memberId, member.copy(level = LevelEnum.safe))))
          case "down" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "降级环境成员", member))
            Ok(Json.toJson(EnvironmentMemberHelper.update(memberId, member.copy(level = LevelEnum.unsafe))))
          case "remove" =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "剔除环境成员", member))
            Ok(Json.toJson(EnvironmentMemberHelper.delete(memberId)))
        }
      case None => NotFound
    }
  }

}
