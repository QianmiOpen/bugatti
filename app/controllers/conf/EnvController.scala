package controllers.conf

import enums.{RoleEnum}
import models.conf.{UserHelper}
import play.api.mvc._
import controllers.BaseController
import enums.{FuncEnum, LevelEnum}
import models.conf.{Environment, EnvironmentHelper}
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
/**
 * 环境管理
 *
 * @author of546
 */
object EnvController extends BaseController {

  implicit val envWrites = Json.writes[Environment]

  val envForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText(maxLength = 30),
      "remark" -> optional(text(maxLength = 250)),
      "nfServer" -> optional(text(maxLength = 30)),
      "ipRange" -> optional(nonEmptyText(maxLength = 300)),
      "level" -> enums.form.enum(LevelEnum)
    )(Environment.apply)(Environment.unapply)
  )

  def show(id: Int) = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentHelper.findById(id)))
  }

  def index(page: Int, pageSize: Int) = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentHelper.all(page, pageSize)))
  }

  def all = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentHelper.all()))
  }

  def showAuth(userName: String) = Action {
    var seq = Seq.empty[Environment]
    UserHelper.findByJobNo(userName) match {
      case Some(user) => {
        user.role match {
          case RoleEnum.user => {
            seq = EnvironmentHelper.findUnsafe()
          }
          case RoleEnum.admin => {
            seq = EnvironmentHelper.all()
          }
        }
      }
      case _ =>
    }
    Ok(Json.toJson(seq))
  }

  def count = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentHelper.count))
  }

  def delete(id: Int) = AuthAction(FuncEnum.env) {
    Ok(Json.toJson(EnvironmentHelper.delete(id)))
  }

  def save = AuthAction(FuncEnum.env) { implicit request =>
    envForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      env =>
        EnvironmentHelper.findByName(env.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
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
            Ok(Json.obj("r" -> Json.toJson(EnvironmentHelper.update(id, env))))
        }
    )
  }

}
