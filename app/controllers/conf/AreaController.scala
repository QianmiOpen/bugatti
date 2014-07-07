package controllers.conf

import models.conf.{AreaInfo, AreaHelper, Area}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import utils.SaltTools

/**
 * Created by mind on 7/6/14.
 */
object AreaController extends Controller{
  implicit val areaFormat = Json.format[Area]
  implicit val areaInfoFormat = Json.format[AreaInfo]

  val areaForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText(maxLength = 30),
      "syndicName" -> nonEmptyText(maxLength = 30),
      "syndicIp" -> nonEmptyText(maxLength = 30)
    )(Area.apply)(Area.unapply)
  )

  def all = Action {
    Ok(Json.toJson(AreaHelper.allInfo))
  }

  def get(id: Int) = Action {
    Ok(Json.toJson(AreaHelper.findInfoById(id)))
  }

  def save = Action { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      area =>
        AreaHelper.findByName(area.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> Json.toJson(AreaHelper.create(area))))
        }
    )
  }

  def update = Action { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      area =>
        Ok(Json.obj("r" -> Json.toJson(AreaHelper.update(area))))
    )
  }

  def delete(id: Int) = Action { implicit request =>
    Ok(Json.obj("r" -> Json.toJson(AreaHelper.delete(id))))
  }

  def refresh(id: Int) = Action { implicit request =>
    AreaHelper.findById(id) match {
      case Some(area) => {
        SaltTools.refreshHost(area.syndicName)
        Ok(Json.obj("r" -> Json.toJson(AreaHelper.findInfoById(id))))
      }
      case None =>{
        Ok(Json.obj("r" -> Json.toJson(0)))
      }
    }
  }
}
