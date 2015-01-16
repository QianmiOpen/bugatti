package controllers.conf

import java.nio.charset.StandardCharsets
import java.nio.file.Files

import actor.ActorUtils
import actor.git.UpdateUser
import controllers.BaseController
import enums.{ModEnum, FuncEnum, RoleEnum}
import exceptions.UniqueNameException
import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json._
import utils.SecurityUtil

/**
 * 用户管理
 *
 * @author of546
 */
object UserController extends BaseController {

  implicit val userWrites = Json.writes[User]
  implicit val permissionWrites = Json.writes[Permission]

  def msg(user: String, ip: String, msg: String, data: User) =
    Json.obj("mod" -> ModEnum.user.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val userForm = Form(
    mapping(
      "jobNo" -> nonEmptyText,
      "name" -> nonEmptyText,
      "role" -> enums.form.enum(RoleEnum),
      "superAdmin" -> boolean,
      "locked" -> boolean,
      "lastIp" -> optional(text),
      "lastVisit" -> optional(jodaDate("yyyy-MM-dd HH:mm:ss")),
      "sshKey" -> optional(text),
      "functions" ->  text
//      "functions" ->  text.verifying("Bad phone number", {_.grouped(2).size == 5})
    )(UserForm.apply)(UserForm.unapply)
  )

  def show(jobNo: String) = AuthAction(FuncEnum.user) { implicit request =>
    Ok(Json.toJson(UserHelper.findByJobNo(jobNo)))
  }

  def index(jobNo: Option[String], page: Int, pageSize: Int) = AuthAction(FuncEnum.user) { implicit request =>
    if (request.user.role == RoleEnum.admin) {
      Ok(Json.toJson(UserHelper.all(jobNo.filterNot(_.isEmpty), page, pageSize)))
    } else {
      Ok(Json.toJson(UserHelper.all(Some(request.user.jobNo), page, pageSize)))
    }
  }

  def count(jobNo: Option[String]) = AuthAction(FuncEnum.user) { implicit request =>
    if (request.user.role == RoleEnum.admin) {
      Ok(Json.toJson(UserHelper.count(jobNo.filterNot(_.isEmpty))))
    } else {
      Ok(Json.toJson(UserHelper.count(Some(request.user.jobNo))))
    }
  }

  def permissions(jobNo: String) = AuthAction(FuncEnum.user) {
    Ok(Json.toJson(PermissionHelper.findByJobNo(jobNo.toLowerCase)))
  }

  def delete(jobNo: String) = AuthAction(FuncEnum.user) { implicit request =>
    UserHelper.findByJobNo(jobNo.toLowerCase) match {
      case Some(user) =>
        if (request.user.role == RoleEnum.user) Forbidden
        else if (user.role == RoleEnum.admin && request.user.superAdmin == false) Forbidden
        else {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除用户", user))
          Ok(Json.toJson(UserHelper.delete(user.jobNo)))
        }
      case None => NotFound
    }
  }

  def save = AuthAction(FuncEnum.user) { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      userForm => {
        val toUser = userForm.toUser
        if (request.user.role == RoleEnum.user) Forbidden
        else if (toUser.role == RoleEnum.admin && request.user.superAdmin == false) Forbidden
        else if (toUser.role == RoleEnum.user && toUser.superAdmin == true) BadRequest
        else {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增用户", toUser))
          try {
            Ok(Json.toJson(UserHelper.create(toUser.copy(jobNo = toUser.jobNo.toLowerCase), userForm.toPermission)))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        }
      }
    )
  }

  def update(jobNo: String) = AuthAction(FuncEnum.user) { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      userForm => {
        val toUser = userForm.toUser
        if (request.user.role == RoleEnum.user) Forbidden
        else if (toUser.role == RoleEnum.admin && request.user.superAdmin == false) Forbidden
        else if (toUser.role == RoleEnum.user && toUser.superAdmin == true) BadRequest
        else {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改用户", toUser))
          try {
            Ok(Json.toJson(UserHelper.update(jobNo.toLowerCase, toUser, userForm.toPermission)))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        }
      }
    )
  }

  def upload(jobNo: String) = AuthAction[TemporaryFile](FuncEnum.user) { implicit request =>
    if (UserHelper.superAdmin_?(request.user) || request.user.jobNo == jobNo) {
      val result = request.body.asMultipartFormData.map { body =>
        val fileContent = body.file("myFile").filter(f => f.ref.file.length() < 1 * 1024 * 1024).map { tempFile =>
          new String(Files.readAllBytes(tempFile.ref.file.toPath), StandardCharsets.UTF_8)
        }
        (UserHelper.findByJobNo(jobNo), fileContent) match {
          case (Some(user), Some(value)) =>
            ActorUtils.keyGit ! UpdateUser(user.copy(sshKey = Some(value)))
            val update2user = user.copy(sshKey = Some(SecurityUtil.encryptUK(value)))
            UserHelper.update(jobNo, update2user)
          case _ => 0
        }
      }
      Ok(if (result == Some(1)) _Success else _Fail)
    } else Forbidden
  }

}
