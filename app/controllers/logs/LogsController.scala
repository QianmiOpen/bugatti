package controllers.logs


import controllers.BaseController
import models.logs.{LogsHelper, Logs}
import org.joda.time.DateTime
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json._

/**
 * log日志
 */
object LogsController extends BaseController {

  implicit val writer = new Writes[(Int, String, String)] {
    def writes(c: (Int, String, String)): JsValue = {
      Json.obj("id" -> c._1, "time" -> c._2, "info" -> c._3)
    }
  }

  val logsForm = Form(
    mapping(
      "jobNo" -> optional(text),
      "mode" -> optional(text),
      "startTime" -> jodaDate("yyyy-MM-dd HH:mm:ss"),
      "endTime" -> jodaDate("yyyy-MM-dd HH:mm:ss")
    )(Logs.apply)(Logs.unapply)
  )

  def search = Action { implicit request =>
    logsForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      logs => {
        Ok(Json.toJson(LogsHelper.searchLogs(logs)))
      }
    )
  }

  def count = Action { implicit request =>
    logsForm.bindFromRequest.fold(
      formWithErrors => BadRequest(Json.obj("r" -> formWithErrors.errorsAsJson)),
      logs => {
        Ok(Json.toJson(LogsHelper.searchCount(logs)))
      }
    )
  }

}
