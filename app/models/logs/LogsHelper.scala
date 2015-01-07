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

  def searchLogs(logs: Logs, page: Int, pageSize: Int): Seq[(Int, String, String)] = db withSession { implicit session =>
    val offset = pageSize * page
    val select = "select event_id, timestmp, formatted_message from logging_event "
    val and = " and timestmp > ? and timestmp < ? order by event_id desc"
    (logs.jobNo, logs.mode) match {
      case (Some(jobNo), Some(mode)) =>
        val sql = Q.query[(String, String, String), (Int, String, String)](select + """where formatted_message like ?""" + and + s" limit ${offset}, ${pageSize}")
        sql.apply(s"""{"mod":"${mode}","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).list
      case (Some(jobNo), None) =>
        val sql = Q.query[(String, String, String), (Int, String, String)](select + """where formatted_message like ?""" + and + s" limit ${offset}, ${pageSize}")
        sql.apply(s"""{"mod":"%","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).list
      case (None, Some(mode)) =>
        val sql = Q.query[(String, String, String), (Int, String, String)](select + """where formatted_message like ?""" + and + s" limit ${offset}, ${pageSize}")
        sql.apply(s"""{"mod":"${mode}","user":"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).list
      case (None, None) =>
        val sql = Q.query[(String, String), (Int, String, String)](select + and.replaceFirst("and", "where") + s" limit ${offset}, ${pageSize}")
        sql.apply(logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).list
    }
  }

  def searchCount(logs: Logs): Int = db withSession { implicit session =>
    val select = "select count(1) from logging_event "
    val and = " and timestmp > ? and timestmp < ?"
    (logs.jobNo, logs.mode) match {
      case (Some(jobNo), Some(mode)) =>
        val sql = Q.query[(String, String, String), Int](select + """where formatted_message like ? """ + and)
        sql.apply(s"""{"mod":"${mode}","user":"${jobNo}"%}""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).first
      case (Some(jobNo), None) =>
        val sql = Q.query[(String, String, String), Int](select + """where formatted_message like ? """ + and)
        sql.apply(s"""{"mod":"%","user":"${jobNo}"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).first
      case (None, Some(mode)) =>
        val sql = Q.query[(String, String, String), Int](select + """where formatted_message like ? """ + and)
        sql.apply(s"""{"mod":"${mode}","user":"%""", logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).first
      case (None, None) =>
        val sql = Q.query[(String, String), Int](select + and.replaceFirst("and", "where"))
        sql.apply(logs.startTime.getMillis.toString, logs.endTime.getMillis.toString).first
    }
  }

}
