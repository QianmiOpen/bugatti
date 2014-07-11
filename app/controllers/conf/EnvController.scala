package controllers.conf

import enums.{RoleEnum}
import models.conf.{MemberHelper, Environment, EnvironmentHelper}
import play.api.mvc._
import controllers.BaseController
import enums.{FuncEnum, LevelEnum}
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

  def showAuth = AuthAction(FuncEnum.env) { implicit request =>
    //管理员 & 委员长 显示所有环境
    var seq = Seq.empty[Environment]
    val user = request.user
    val countSafe = MemberHelper.countByJobNo_Level(user.jobNo, LevelEnum.safe)
    if(user.role == RoleEnum.admin || countSafe > 0){
      seq = EnvironmentHelper.all()
    } else {
      seq = EnvironmentHelper.findByUnsafe()
    }
    Ok(Json.toJson(seq))
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
