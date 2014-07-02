package controllers.conf

import models.conf._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 项目版本
 *
 * @author of546
 */
object VersionController extends Controller {

  implicit val versionWrites = Json.writes[Version]

  val versionForm = Form(
    mapping(
      "id" -> optional(number),
      "pid" -> number,
      "vs" -> nonEmptyText,
      "updated" -> default(jodaDate("yyyy-MM-dd hh:mm:ss"), DateTime.now())
    )(Version.apply)(Version.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(VersionHelper.findById(id)))
  }

  def index(pid: Int, page: Int, pageSize: Int) = Action {
    Ok(Json.toJson(VersionHelper.all(pid, page, pageSize)))
  }

  def count(pid: Int) = Action {
    Ok(Json.toJson(VersionHelper.count(pid)))
  }

  def all(pid: Int, top: Int) = Action {
    Ok(Json.toJson(VersionHelper.all(pid, top)))
  }

  def delete(id: Int) = Action {
    VersionHelper.findById(id) match {
      case Some(version) =>
        // todo fix version permission, return Forbidden
        Ok(Json.toJson(VersionHelper.delete(version)))
      case None =>
        NotFound
    }
  }

  def save = Action { implicit request =>
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

  def update(id: Int) = Action { implicit request =>
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
