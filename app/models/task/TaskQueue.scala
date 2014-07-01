package models.task

import org.joda.time._
import play.api.Logger
import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskQueue(id: Option[Int], envId: Int, projectId: Int, version: String, taskTemplateId:Int, status: Int, importTime: DateTime, taskId: Option[Int], operatorId: Int)

case class TaskQueueTable(tag: Tag) extends Table[TaskQueue](tag, "task_queue") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.NotNull)
  def projectId = column[Int]("project_id", O.NotNull)
  def version = column[String]("version", O.NotNull, O.DBType("VARCHAR(64)"))
  def taskTemplateId = column[Int]("task_template_id",O.NotNull)
  def status = column[Int]("status", O.NotNull)
  def importTime = column[DateTime]("import_time", O.NotNull, O.DBType("DATETIME"))
  def taskId = column[Int]("task_id", O.Nullable)
  def operatorId = column[Int]("operator_id", O.NotNull)

  override def * = (id.?, envId, projectId, version, taskTemplateId, status, importTime, taskId.?, operatorId) <> (TaskQueue.tupled, TaskQueue.unapply _)
}

object TaskQueueHelper{
  import models.AppDB._

  val qTaskQueue = TableQuery[TaskQueueTable]

  implicit val taskQueueReader : Reads[TaskQueue] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "envId").read[Int] and
    (JsPath \ "projectId").read[Int] and
    (JsPath \ "version").read[String] and
    (JsPath \ "taskTemplateId").read[Int] and
    (JsPath \ "status").read[Int] and
    (JsPath \ "importTime").read[DateTime] and
    (JsPath \ "taskId").readNullable[Int] and
    (JsPath \ "operatorId").read[Int]
  )(TaskQueue.apply _)

  /**
   * 查询应该被执行的任务
   * @param envId
   * @param projectId
   * @return
   */
  def findExecuteTask(envId: Int, projectId: Int): TaskQueue = db withSession { implicit session =>
    val minTime = qTaskQueue.where(_.envId === envId).where(_.projectId === projectId).groupBy(_.projectId).map{
      case (id, row) => row.map(_.importTime).min
    }.firstOption
    Logger.info("time is " + minTime)
    if(minTime != None){
      qTaskQueue.where(_.envId === envId).where(_.projectId === projectId).where(_.importTime === minTime.get).first
    }
    else {
      null
    }
  }

  def updateStatus(tq: TaskQueue, taskId: Int) = db withSession{ implicit session =>
    qTaskQueue.where(_.id === tq.id).update(tq.copy(status = 3).copy(taskId = Option(taskId)))
  }

  def add(tq: TaskQueue): Int = db withSession{ implicit session =>
    Logger.info("insert into taskQueue")
    val taskQueueId = qTaskQueue.returning(qTaskQueue.map(_.id)).insert(tq)
    taskQueueId
  }

  def remove(tq: TaskQueue): Int = db withSession{ implicit session =>
    Logger.info("remove from taskQueue")
    qTaskQueue.where(_.id === tq.id).delete
  }

}