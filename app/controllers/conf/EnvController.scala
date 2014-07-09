package controllers.conf

import enums.{RoleEnum, LevelEnum}
import models.conf.{UserHelper, Environment, EnvironmentHelper}
import play.api.Logger
import play.api.mvc._
import play.api.libs.json._
import play.api.data._
import play.api.data.Forms._
/**
 * 环境管理
 *
 * @author of546
 */
object EnvController extends Controller {

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

  def show(id: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.findById(id)))
  }

  def index(page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.all(page, pageSize)))
  }

  def all = Action {
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
  def count = Action {
    Ok(Json.toJson(EnvironmentHelper.count))
  }

  def delete(id: Int) = Action {
    Ok(Json.toJson(EnvironmentHelper.delete(id)))
  }

  def save = Action { implicit request =>
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

  def update(id: Int) = Action { implicit request =>
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
