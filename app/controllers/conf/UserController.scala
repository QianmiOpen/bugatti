package controllers.conf

import controllers.BaseController
import controllers.conf.RelationController._
import enums.{LevelEnum, FuncEnum, RoleEnum}
import models.conf._
import org.joda.time.DateTime
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

  def show(jobNo: String) = AuthAction(FuncEnum.user) {
    Ok(Json.toJson(UserHelper.findByJobNo(jobNo)))
  }

  def index(page: Int, pageSize: Int) = AuthAction(FuncEnum.user) {
    Ok(Json.toJson(UserHelper.all(page, pageSize)))
  }

  def count = AuthAction(FuncEnum.user) {
    Ok(Json.toJson(UserHelper.count))
  }

  def permissions(jobNo: String) = AuthAction(FuncEnum.user) {
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

  // ---------------------------------------------------
  // 项目和环境资源权限
  // ---------------------------------------------------
  def hasProject(projectId: Int, user: User): Boolean = {
    if (user.role == RoleEnum.admin) true
    else MemberHelper.findByPid_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.pid == projectId => true
      case _ => false
    }
  }

  def hasProjectInEnv(projectId: Int, envId: Int, user: User): Boolean = {
    if (user.role == RoleEnum.admin) true
    else MemberHelper.findByPid_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.pid == projectId =>
        EnvironmentHelper.findById(envId) match {
          case Some(env) if env.level == LevelEnum.safe => if (member.level == env.level) true else false
          case Some(env) if env.level == LevelEnum.unsafe => true
          case None => false
        }
      case _ => false
    }
  }

}
