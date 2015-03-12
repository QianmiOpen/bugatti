package controllers.admin

import actor.ActorUtils
import actor.salt._
import controllers.BaseController
import enums.ModEnum
import exceptions.UniqueNameException
import models.conf.{Spirit, SpiritHelper}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json

/**
 * Created by mind on 1/12/15.
 */
object SpiritController extends BaseController {
  implicit val jsonFormat = Json.format[Spirit]

  def msg(user: String, ip: String, msg: String, data: Spirit) =
    Json.obj("mod" -> ModEnum.spirit.toString, "user" -> user, "ip" -> ip, "msg" -> msg, "data" -> Json.toJson(data)).toString

  val spiritForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText(maxLength = 60),
      "ip" -> nonEmptyText(maxLength = 16),
      "info" -> optional(text(maxLength = 255))
    )(Spirit.apply)(Spirit.unapply)
  )

  def all = AuthAction() {
    Ok(Json.toJson(SpiritHelper.all))
  }

  def get(id: Int) = AuthAction() {
    Ok(Json.toJson(SpiritHelper.findById(id)))
  }

  def add = AuthAction() { implicit request =>
    spiritForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      spirit =>
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "新增网关", spirit))

          val spiritId = SpiritHelper.create(spirit)
          ActorUtils.spirits ! AddSpirit(spirit.copy(id = Option(spiritId)))

          Ok(Json.toJson(spiritId))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
    )
  }

  def update(id: Int) = AuthAction() { implicit request =>
    spiritForm.bindFromRequest.fold(
      formWithErrors => BadRequest(formWithErrors.errorsAsJson),
      spirit =>
        try {
          ALogger.info(msg(request.user.jobNo, request.remoteAddress, "修改网关", spirit))

          val tmpSpirit = spirit.copy(id = Option(id))
          val result = SpiritHelper.update(tmpSpirit)
          ActorUtils.spirits ! UpdateSpirit(tmpSpirit)

          Ok(Json.toJson(tmpSpirit))
        } catch {
          case un: UniqueNameException => Ok(_Exist)
        }
    )
  }

  def delete(id: Int) = AuthAction() { implicit request =>
    SpiritHelper.findById(id) match {
      case Some(spirit) =>
        ALogger.info(msg(request.user.jobNo, request.remoteAddress, "删除网关", spirit))
        val result = SpiritHelper.delete(id)
        ActorUtils.spirits ! DeleteSpirit(id)
        Ok(Json.toJson(result))
      case None => NotFound
    }
  }

  def refresh(id: Int) = AuthAction() { implicit request =>
    ActorUtils.spiritsRefresh ! RefreshSpiritsActor.RefreshHosts(id)
    Ok
  }
}
