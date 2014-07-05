package controllers.conf

import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 项目类型
 */
object TemplateController extends Controller {

  implicit val templateWrites = Json.writes[Template]
  implicit val templateItemsWrites = Json.writes[TemplateItem]

  val templateForm = Form(
    mapping(
      "id" -> optional(number),
      "name" -> nonEmptyText,
      "remark" -> optional(text),
      "items" -> list(
        mapping(
          "id" -> optional(number),
          "tid" -> optional(number),
          "itemName" -> nonEmptyText,
          "itemDesc" -> optional(text),
          "order" -> number
        )(TemplateItem.apply)(TemplateItem.unapply)
      )
    )(TemplateFrom.apply)(TemplateFrom.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(TemplateHelper.findById(id)))
  }

  def delete(id: Int) = Action {
    ProjectHelper.countByTid(id) match {
      case count if count > 0 => Ok(Json.obj("r" -> "exist")) // 项目中还存在使用情况
      case _ => Ok(Json.obj("r" -> Json.toJson(TemplateHelper.delete(id))))
    }
  }

  def all = Action { implicit request =>
    Ok(Json.toJson(TemplateHelper.all))
  }

  def save = Action { implicit request =>
    templateForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      templateFrom => {
        TemplateHelper.findByName(templateFrom.name) match {
          case Some(_) =>
            Ok(Json.obj("r" -> "exist"))
          case None =>
            Ok(Json.obj("r" -> TemplateHelper.create(templateFrom.toTemplate, templateFrom.items)))
        }
      }
    )
  }

  def update(id: Int) = Action { implicit request =>
    templateForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      templateFrom => {
        Ok(Json.obj("r" -> TemplateHelper.update(id, templateFrom.toTemplate, templateFrom.items)))
      }
    )
  }

  // 模板，所有属性
  def items(tid: Int) = Action { implicit request =>
    Ok(Json.toJson(TemplateItemHelper.findByTid(tid)))
  }

}
