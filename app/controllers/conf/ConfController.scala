package controllers.conf

import models.conf._
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import play.api.libs.json._

/**
 * 配置文件
 */
object ConfController extends Controller {

  implicit val confWrites = Json.writes[Conf]
  implicit val contentWrites = Json.writes[ConfContent]

  val confForm = Form(
    mapping(
      "id" -> optional(number),
      "eid" -> number,
      "pid" -> number,
      "vid" -> number,
      "name" -> optional(text),
      "path" -> nonEmptyText,
      "content" -> default(text, ""),
      "remark" -> optional(text),
      "lastUpdated" -> default(jodaDate("yyyy-MM-dd hh:mm:ss"), DateTime.now())
    )(ConfForm.apply)(ConfForm.unapply)
  )

  def show(id: Int) = Action {
    ConfHelper.findById(id) match {
      case Some(conf) =>
        Ok(Json.obj("conf" -> Json.toJson(conf), "content" -> ConfContentHelper.findById(id)))
      case None =>
        NotFound
    }
  }

  def all(eid: Int, vid: Int) = Action {
    Ok(Json.toJson(ConfHelper.findByEid_Vid(eid, vid)))
  }

  def save = Action { implicit request =>
    confForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      confForm => {
        Ok(Json.obj("r" -> Json.toJson(ConfHelper.create(confForm))))
      }
    )
  }

}
