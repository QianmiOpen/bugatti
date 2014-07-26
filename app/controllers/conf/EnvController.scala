package controllers.conf

import enums.{ModEnum, RoleEnum, FuncEnum, LevelEnum}
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
      "remark" -> optional(text(maxLength = 250)),
      "nfServer" -> optional(text(maxLength = 30)),
      "ipRange" -> optional(nonEmptyText(maxLength = 300)),
      "level" -> enums.form.enum(LevelEnum),
      "scriptVersion" -> nonEmptyText(maxLength = 30),
      "variable" -> seq (
        mapping(
          "name" -> text,
          "value" -> text
        )(Variable.apply)(Variable.unapply)
      )
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

  // 任务模块查看
  def showAuth = AuthAction(FuncEnum.task) { implicit request =>
    // 管理员 & 委员长 显示所有环境
    val countSafe = MemberHelper.count(request.user.jobNo, LevelEnum.safe)
    val seq =
      if (request.user.role == RoleEnum.admin || countSafe > 0) EnvironmentHelper.all()
      else EnvironmentHelper.findByUnsafe()
    Ok(Json.toJson(seq))
  }

  def delete(id: Int) = AuthAction(FuncEnum.env) { implicit request =>
    EnvironmentHelper.findById(id) match {
      case Some(env) =>
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除环境", env))
        Ok(Json.toJson(EnvironmentHelper.delete(id)))
      case None =>
        NotFound
    }
  }

  def allScriptVersion = AuthAction(FuncEnum.env) { implicit request =>
    Ok(Json.toJson(ScriptVersionHelper.allName))
  }

  def save = AuthAction(FuncEnum.env) { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      env =>
        EnvironmentHelper.findByName(env.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增环境", env))
            Ok(Json.obj("r" -> Json.toJson(EnvironmentHelper.create(env))))
        }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.env) { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      env =>
        EnvironmentHelper.findByName(env.name).find(_.id != Some(id)) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改环境", env))
            Ok(Json.obj("r" -> Json.toJson(EnvironmentHelper.update(id, env))))
        }
    )
  }

}
