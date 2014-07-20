package models.task

import enums.TaskEnum.TaskStatus
import org.joda.time.DateTime
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskScheme(id: Option[Int], envId: Int, projectId: Int, versionId: Option[Int], taskTemplateId:Int, status: TaskStatus, startTime: DateTime, operatorId: Int)

class TaskSchemeTable(tag: Tag) extends Table[TaskScheme](tag, "task_scheme") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def projectId = column[Int]("project_id")
  def versionId = column[Int]("version_id", O.Nullable)
  def taskTemplateId = column[Int]("task_template_id")
  def status = column[TaskStatus]("status")
  def startTime = column[DateTime]("start_time", O.DBType("DATETIME"))
  def operatorId = column[Int]("operator_id")

  override def * = (id.?, envId, projectId, versionId.?, taskTemplateId, status, startTime, operatorId) <> (TaskScheme.tupled, TaskScheme.unapply _)

}

object TaskSchemeHelper{

}