package controllers.conf

import controllers.BaseController
import enums.{FuncEnum, RoleEnum}
import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 用户管理
 *
 * @author of546
 */
object UserController extends BaseController {

  implicit val userWrites = Json.writes[User]
  implicit val permissionWrites = Json.writes[Permission]

  val userForm = Form(
    mapping(
      "jobNo" -> nonEmptyText,
      "name" -> nonEmptyText,
      "role" -> enums.form.enum(RoleEnum),
      "locked" -> boolean,
      "lastIp" -> optional(text),
      "lastVisit" -> optional(jodaDate("yyyy-MM-dd hh:mm:ss")),
      "functions" ->  text
//      "functions" ->  text.verifying("Bad phone number", {_.grouped(2).size == 5})
    )(UserForm.apply)(UserForm.unapply)
  )

  def show(jobNo: String) = Action {
    Ok(Json.toJson(UserHelper.findByJobNo(jobNo)))
  }

  def index(page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(UserHelper.all(page, pageSize)))
  }

  def count = Action {
    Ok(Json.toJson(UserHelper.count))
  }

  def permissions(jobNo: String) = Action {
    Ok(Json.toJson(PermissionHelper.findByJobNo(jobNo)))
  }

  def delete(jobNo: String) = AuthAction(FuncEnum.user) {
    Ok(Json.toJson(UserHelper.delete(jobNo)))
  }

  def save = AuthAction(FuncEnum.user) { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      userForm => {
        UserHelper.findByJobNo(userForm.jobNo) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> UserHelper.create(userForm.toUser, userForm.toPermission)))
        }
      }
    )
  }

  def update(jobNo: String) = AuthAction(FuncEnum.user) { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      userForm => {
        Ok(Json.obj("r" -> UserHelper.update(jobNo, userForm.toUser, userForm.toPermission)))
      }
    )
  }

}
