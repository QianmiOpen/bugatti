package controllers.conf

import controllers.BaseController
import controllers.conf.UserController._
import enums.FuncEnum
import models.conf._
import org.joda.time.DateTime
import play.api.Logger
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 项目版本
 *
 * @author of546
 */
object VersionController extends BaseController {

  implicit val versionWrites = Json.writes[Version]

  val versionForm = Form(
    mapping(
      "id" -> optional(number),
      "pid" -> number,
      "vs" -> nonEmptyText,
      "updated" -> default(jodaDate("yyyy-MM-dd hh:mm:ss"), DateTime.now())
    )(Version.apply)(Version.unapply)
  )

  def show(id: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(VersionHelper.findById(id)))
  }

  def index(pid: Int, page: Int, pageSize: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(VersionHelper.all(pid, page, pageSize)))
  }

  def count(pid: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(VersionHelper.count(pid)))
  }

  def all(pid: Int, top: Int) = AuthAction(FuncEnum.project) {
    Ok(Json.toJson(VersionHelper.all(pid, top)))
  }

  def delete(id: Int) = AuthAction(FuncEnum.project) {
    VersionHelper.findById(id) match {
      case Some(version) =>
        // todo  version permission, return Forbidden

        ConfHelper.findByVid(id).isEmpty match {
          case true =>
            Ok(Json.obj("r" -> Json.toJson(VersionHelper.delete(version))))
          case false =>
            Ok(Json.obj("r" -> "exist"))
        }
      case None =>
        Ok(Json.obj("r" -> "none"))
    }
  }

  def save = AuthAction(FuncEnum.project) { implicit request =>
    versionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      versionForm => {
        VersionHelper.findByPid(versionForm.pid).find(_.vs == versionForm.vs) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" ->Json.toJson(VersionHelper.create(versionForm))))
        }
      }
    )
  }

  def update(id: Int) = AuthAction(FuncEnum.project) { implicit request =>
    versionForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      versionForm => {
        VersionHelper.findByPid(versionForm.pid)
          .filterNot(_.id == versionForm.id) // Some(id)
          .find(_.vs == versionForm.vs) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> Json.toJson(VersionHelper.update(id, versionForm))))
        }
      }
    )
  }

}
