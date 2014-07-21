package models.logs

import org.joda.time.DateTime
import play.api.Play.current
import scala.slick.jdbc.{StaticQuery => Q}

/**
 * 日志查询
 */
case class Logs(jobNo: Option[String], mode: Option[String], startTime: DateTime, endTime: DateTime)

object LogsHelper {

  import models.AppDB._

  def searchLogs(logs: Logs): Seq[(Int, String, String)] = db withSession { implicit session =>
    val select = "select event_id, timestmp, formatted_message from logging_event "
    val and = " and timestmp > ? and timestmp < ? order by event_id desc"
    (logs.jobNo, logs.mode) match {
      case (Some(jobNo), Some(mode)) =>
        val sql = Q[(String, String, String), (Int, String, String)] +
          select + """where formatted_message like ?""" + and
        sql.list(s"""{"mod":"${mode}","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (Some(jobNo), None) =>
        val sql = Q[(String, String, String), (Int, String, String)] +
          select + """where formatted_message like ?""" + and
        sql.list(s"""{"mod":"%","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (None, Some(mode)) =>
        val sql = Q[(String, String, String), (Int, String, String)] +
          select + """where formatted_message like ?""" + and
        sql.list(s"""{"mod":"${mode}","user":"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (None, None) =>
        val sql = Q[(String, String), (Int, String, String)] +
          select + and.replaceFirst("and", "where")
        sql.list(logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
    }
  }

  def searchCount(logs: Logs): Int = db withSession { implicit session =>
    val select = "select count(1) from logging_event "
    val and = " and timestmp > ? and timestmp < ?"
    (logs.jobNo, logs.mode) match {
      case (Some(jobNo), Some(mode)) =>
        val sql = Q[(String, String, String), Int] +
          select + """where formatted_message like ? """ + and
        sql.first(s"""{"mod":"${mode}","user":"${jobNo}"%}""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (Some(jobNo), None) =>
        val sql = Q[(String, String, String), Int] +
          select + """where formatted_message like ? """ + and
        sql.first(s"""{"mod":"%","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (None, Some(mode)) =>
        val sql = Q[(String, String, String), Int] +
          select + """where formatted_message like ? """ + and
        sql.first(s"""{"mod":"${mode}","user":"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
      case (None, None) =>
        val sql = Q[(String, String), Int] +
          select + and.replaceFirst("and", "where")
        sql.first(logs.startTime.getMillis.toString, logs.endTime.getMillis.toString)
    }
  }

}
