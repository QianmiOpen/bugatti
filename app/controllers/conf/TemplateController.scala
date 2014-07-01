package controllers.conf

import play.api.mvc.Controller
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

  // 项目所有类型
  def all = Action { implicit request =>
    Ok(Json.toJson(TemplateHelper.all))
  }


}
