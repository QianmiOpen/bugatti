package controllers.conf

import controllers.BaseController
import enums.ItemTypeEnum
import enums.ItemTypeEnum.ItemType
import models.conf._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 项目模板类型
 *
 * @author of546
 */
object TemplateController extends BaseController {

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
          "templateId" -> optional(number),
          "itemName" -> nonEmptyText,
          "itemDesc" -> optional(text),
          "itemType" -> enums.form.enum(ItemTypeEnum),
          "default" -> optional(text),
          "order" -> number,
          "scriptVersion" -> nonEmptyText
        )(TemplateItem.apply)(TemplateItem.unapply)
      )
    )(TemplateFrom.apply)(TemplateFrom.unapply)
  )

  def show(id: Int) = Action {
    Ok(Json.toJson(TemplateHelper.findById(id)))
  }

  def all = Action { implicit request =>
    Ok(Json.toJson(TemplateHelper.all))
  }

  def delete(id: Int) = Action {
    ProjectHelper.countByTemplateId(id) match {
      case count if count > 0 => Ok(Json.obj("r" -> "exist")) // 项目中还存在使用情况
      case _ =>
        Ok(Json.obj("r" -> Json.toJson(TemplateHelper.delete(id))))
    }
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

  // --------------------------------------------
  // 模板属性
  // --------------------------------------------
  def items(templateId: Int) = Action { implicit request =>
    Ok(Json.toJson(TemplateItemHelper.findByTemplateId(templateId)))
  }

  def itemAttrs(templateId: Int, scriptVersion: String) = Action { implicit request =>
    Ok(Json.toJson(TemplateItemHelper.findByItemType(templateId, scriptVersion, ItemTypeEnum.attribute)))
  }

  def itemVars(templateId: Int, scriptVersion: String) = Action { implicit request =>
    Ok(Json.toJson(TemplateItemHelper.findByItemType(templateId, scriptVersion, ItemTypeEnum.variable)))
  }

}
