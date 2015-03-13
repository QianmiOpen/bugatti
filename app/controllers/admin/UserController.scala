package controllers.admin

import actor.ActorUtils
import actor.git.UpdateUser
import controllers.BaseController
import enums.{ModEnum, RoleEnum}
import exceptions.UniqueNameException
import models.conf._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.json._
import utils.SecurityUtil

/**
 * 用户管理
 *
 * @author of546
 */
object UserController extends BaseController {

  implicit val userWrites = Json.writes[User]

  def msg(user: String, ip: String, msg: String, data: User) =
    Json.obj("mod" -> ModEnum.user.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val userForm = Form(
    mapping(
      "jobNo" -> nonEmptyText,
      "name" -> nonEmptyText,
      "role" -> enums.form.enum(RoleEnum),
      "password" -> optional(text(maxLength = 256)),
      "locked" -> boolean,
      "lastIp" -> optional(text),
      "lastVisit" -> optional(jodaDate("yyyy-MM-dd HH:mm:ss")),
      "sshKey" -> optional(text)
    )(User.apply)(User.unapply)
  )

  def show(jobNo: String) = AuthAction() { implicit request =>
    val user = UserHelper.findByJobNo(jobNo)
    if (UserHelper.admin_?(request.user) || request.user.jobNo == jobNo.toLowerCase) {
      Ok(Json.toJson(user.collect {
        case u =>
          if (u.sshKey.nonEmpty)
            u.copy(sshKey = Some(SecurityUtil.decryptUK(u.sshKey.get)))
          else u
      }))
    } else {
      Ok(Json.toJson(user.collect { case u => u.copy(sshKey = None) }))
    }
  }

  def index(jobNo: Option[String], page: Int, pageSize: Int) = AuthAction(RoleEnum.admin) { implicit request =>
    Ok(Json.toJson(UserHelper.all(jobNo.filterNot(_.isEmpty), page, pageSize)))
  }

  def count(jobNo: Option[String]) = AuthAction(RoleEnum.admin) { implicit request =>
    Ok(Json.toJson(UserHelper.count(jobNo.filterNot(_.isEmpty))))
  }

  def delete(jobNo: String) = AuthAction(RoleEnum.admin) { implicit request =>
    UserHelper.findByJobNo(jobNo.toLowerCase) match {
      case Some(user) => {
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除用户", user))
        Ok(Json.toJson(UserHelper.delete(user.jobNo)))
      }
      case None => NotFound
    }
  }

  def save = AuthAction(RoleEnum.admin) { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      _userForm => {
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增用户", _userForm))
        try {
          val create2user = _userForm.sshKey match {
            case Some(key) =>
              ActorUtils.keyGit ! UpdateUser(_userForm.copy(sshKey = Some(key)))
              _userForm.copy(sshKey = Some(SecurityUtil.encryptUK(key)))
            case None => _userForm
          }
          Ok(Json.toJson(UserHelper.create(create2user.copy(jobNo = _userForm.jobNo.toLowerCase))))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
      }
    )
  }

  def update(jobNo: String) = AuthAction() { implicit request =>
    userForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      _userForm => {
        if (UserHelper.admin_?(request.user) || request.user.jobNo == jobNo) {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改用户", _userForm))
          try {
            val update2user = _userForm.sshKey match {
              case Some(key) =>
                ActorUtils.keyGit ! UpdateUser(_userForm.copy(sshKey = Some(key)))
                _userForm.copy(sshKey = Some(SecurityUtil.encryptUK(key)))
              case None => _userForm
            }
            Ok(Json.toJson(UserHelper.update(jobNo, update2user.copy(jobNo = _userForm.jobNo.toLowerCase))))
          } catch {
            case un: UniqueNameException => Ok(_Exist)
          }
        } else Forbidden
      }
    )
  }

}
