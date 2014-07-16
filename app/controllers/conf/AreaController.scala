package controllers.conf

import controllers.BaseController
import enums.FuncEnum
import models.conf.{AreaInfo, AreaHelper, Area}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import utils.SaltTools

/**
 * 区域管理
 * @author of557
 */
object AreaController extends BaseController {
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

  def save = AuthAction(FuncEnum.area) { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      area =>
        AreaHelper.findByName(area.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            ALogger.info(s"mod:${FuncEnum.area}|user:${request.user.jobNo}|msg:增加区域|data:${Json.toJson(area)}")
            Ok(Json.obj("r" -> Json.toJson(AreaHelper.create(area))))
        }
    )
  }

  def update = AuthAction(FuncEnum.area) { implicit request =>
    areaForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      area => {
        ALogger.info(s"mod:${FuncEnum.area}|user:${request.user.jobNo}|msg:修改区域|data:${Json.toJson(area)}")
        Ok(Json.obj("r" -> Json.toJson(AreaHelper.update(area))))
      }
    )
  }

  def delete(id: Int) = AuthAction(FuncEnum.area) { implicit request =>
    ALogger.info(s"mod:${FuncEnum.area}|user:${request.user.jobNo}|msg:删除区域|data:${id}")
    Ok(Json.obj("r" -> Json.toJson(AreaHelper.delete(id))))
  }

  def refresh(id: Int) = AuthAction(FuncEnum.area) { implicit request =>
    AreaHelper.findById(id) match {
      case Some(area) => {
        SaltTools.refreshHost(area.syndicName)
        ALogger.info(s"mod:${FuncEnum.area}|user:${request.user.jobNo}|msg:刷新区域|data:${Json.toJson(area)}")
        Ok(Json.obj("r" -> Json.toJson(AreaHelper.findInfoById(id))))
      }
      case None =>{
        Ok(Json.obj("r" -> Json.toJson(0)))
      }
    }
  }

}
