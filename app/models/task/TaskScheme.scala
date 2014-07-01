package models.task

import org.joda.time.DateTime
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskScheme(id: Option[Int], envId: Int, projectId: Int, version: String, taskTemplateId:Int, status: Int, startTime: DateTime, operatorId: Int)

class TaskSchemeTable(tag: Tag) extends Table[TaskScheme](tag, "task_scheme") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.NotNull)
  def projectId = column[Int]("project_id", O.NotNull)
  def version = column[String]("version", O.NotNull, O.DBType("VARCHAR(64)"))
  def taskTemplateId = column[Int]("task_template_id",O.NotNull)
  def status = column[Int]("status", O.NotNull)
  def startTime = column[DateTime]("start_time", O.NotNull, O.DBType("DATETIME"))
  def operatorId = column[Int]("operator_id", O.NotNull)

  override def * = (id.?, envId, projectId, version, taskTemplateId, status, startTime, operatorId) <> (TaskScheme.tupled, TaskScheme.unapply _)

}

object TaskSchemeHelper{

}