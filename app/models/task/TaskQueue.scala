package models.task

import enums.TaskEnum
import enums.TaskEnum.TaskStatus
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
case class TaskQueue(id: Option[Int], envId: Int, projectId: Int, versionId: Option[Int], taskTemplateId:Int, status: TaskStatus, importTime: DateTime, taskId: Option[Int], schemeId: Option[Int], operatorId: Int)

case class TaskQueueTable(tag: Tag) extends Table[TaskQueue](tag, "task_queue") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.NotNull)
  def projectId = column[Int]("project_id", O.NotNull)
  def versionId = column[Int]("version_id", O.Nullable)
  def taskTemplateId = column[Int]("task_template_id",O.NotNull)
  def status = column[TaskStatus]("status", O.NotNull)
  def importTime = column[DateTime]("import_time", O.NotNull, O.DBType("DATETIME"))
  def taskId = column[Int]("task_id", O.Nullable)
  def schemeId = column[Int]("scheme_id", O.Nullable)
  def operatorId = column[Int]("operator_id", O.NotNull)

  override def * = (id.?, envId, projectId, versionId.?, taskTemplateId, status, importTime, taskId.?, schemeId.?, operatorId) <> (TaskQueue.tupled, TaskQueue.unapply _)
}

object TaskQueueHelper{
  import models.AppDB._

  val qTaskQueue = TableQuery[TaskQueueTable]

  implicit val taskQueueReader : Reads[TaskQueue] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "envId").read[Int] and
    (JsPath \ "projectId").read[Int] and
    (JsPath \ "versionId").readNullable[Int] and
    (JsPath \ "taskTemplateId").read[Int] and
    (JsPath \ "status").read[TaskStatus] and
    (JsPath \ "importTime").read[DateTime] and
    (JsPath \ "taskId").readNullable[Int] and
    (JsPath \ "schemeId").readNullable[Int] and
    (JsPath \ "operatorId").read[Int]
  )(TaskQueue.apply _)

  /**
   * 查询应该被执行的任务
   * @param envId
   * @param projectId
   * @return
   */
  def findExecuteTask(envId: Int, projectId: Int): Option[TaskQueue] = db withSession { implicit session =>
    val minTime = qTaskQueue.where(_.envId === envId).where(_.projectId === projectId).groupBy(_.projectId).map{
      case (id, row) => row.map(_.importTime).min
    }.firstOption
    Logger.info("time is " + minTime)

    minTime match {
      case Some(time) => {
        qTaskQueue.where(_.envId === envId).where(_.projectId === projectId).where(_.importTime === time).firstOption
      }
      case _ => {
        None
      }
    }
//    if(minTime != None){
//      qTaskQueue.where(_.envId === envId).where(_.projectId === projectId).where(_.importTime === minTime.get).first
//    }
//    else {
//      null
//    }
  }

  def findWaitQueueById(qId: Int): Option[TaskQueue] = db withSession{ implicit session =>
    qTaskQueue.where(_.id is qId).where(_.status is TaskEnum.TaskWait).firstOption
  }
  def findQueueNum(envId: Int, projectId: Int): Int = db withSession {implicit session =>
    //    Query(qTaskQueue.where(_.envId is tq.envId).where(_.projectId is tq.projectId).where(_.status is TaskEnum.TaskWait).length).first
    qTaskQueue.where(_.envId is envId).where(_.projectId is projectId).where(_.status is TaskEnum.TaskWait).length.run
  }

  def findQueues(envId: Int, projectId: Int): List[TaskQueue] = db withSession { implicit session =>
    qTaskQueue.where(_.envId is envId).where(_.projectId is projectId).list
  }

  def findEnvId_ProjectId(): Set[(Int, Int)] = db withSession { implicit session =>
    var set = Set.empty[(Int, Int)]
    qTaskQueue.list.map{
      q =>
        set = set + ((q.envId, q.projectId))
    }
    set
  }

  def create(tq: TaskQueue): Int = db withSession{ implicit session =>
    Logger.info("insert into taskQueue")
    val taskQueueId = qTaskQueue.returning(qTaskQueue.map(_.id)).insert(tq)
    taskQueueId
  }

  def delete(tq: TaskQueue): Int = db withSession{ implicit session =>
    Logger.info("remove from taskQueue")
    qTaskQueue.where(_.id === tq.id).delete
  }

  def updateStatus(tq: TaskQueue, taskId: Int) = db withSession{ implicit session =>
    qTaskQueue.where(_.id === tq.id).update(tq.copy(status = TaskEnum.TaskProcess).copy(taskId = Option(taskId)))
  }

}