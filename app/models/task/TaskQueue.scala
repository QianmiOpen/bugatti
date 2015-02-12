package models.task

import enums.TaskEnum
import enums.TaskEnum.TaskStatus
import enums.TaskExeEnum.TaskExeWay
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
case class TaskQueue(id: Option[Int], envId: Int, projectId: Int, clusterName: Option[String], versionId: Option[Int], taskTemplateId:Int, status: TaskStatus, importTime: DateTime, taskId: Option[Int], schemeId: Option[Int], operatorId: String, force: TaskExeWay)

case class TaskQueueTable(tag: Tag) extends Table[TaskQueue](tag, "task_queue") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def projectId = column[Int]("project_id")
  def clusterName = column[String]("cluster_name", O.Nullable)
  def versionId = column[Int]("version_id", O.Nullable)
  def taskTemplateId = column[Int]("task_template_id")
  def status = column[TaskStatus]("status")
  def importTime = column[DateTime]("import_time", O.DBType("DATETIME"))
  def taskId = column[Int]("task_id", O.Nullable)
  def schemeId = column[Int]("scheme_id", O.Nullable)
  def operatorId = column[String]("operator_id")
  def force = column[TaskExeWay]("force")

  override def * = (id.?, envId, projectId, clusterName.?, versionId.?, taskTemplateId, status, importTime, taskId.?, schemeId.?, operatorId, force) <> (TaskQueue.tupled, TaskQueue.unapply _)
}

object TaskQueueHelper{
  import models.AppDB._

  val qTaskQueue = TableQuery[TaskQueueTable]

  implicit val taskQueueReader : Reads[TaskQueue] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "envId").read[Int] and
    (JsPath \ "projectId").read[Int] and
    (JsPath \ "clusterName").readNullable[String] and
    (JsPath \ "versionId").readNullable[Int] and
    (JsPath \ "taskTemplateId").read[Int] and
    (JsPath \ "status").read[TaskStatus] and
    (JsPath \ "importTime").read[DateTime] and
    (JsPath \ "taskId").readNullable[Int] and
    (JsPath \ "schemeId").readNullable[Int] and
    (JsPath \ "operatorId").read[String] and
    (JsPath \ "force").read[TaskExeWay]
  )(TaskQueue.apply _)

  /**
   * 查询应该被执行的任务
   * @param envId
   * @param projectId
   * @param clusterName
   * @return
   */
  def findExecuteTask(envId: Int, projectId: Int, clusterName: Option[String]): Option[TaskQueue] = db withSession { implicit session =>
    clusterName match {
      case Some(c) => {
        val minTime = qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.clusterName === clusterName).groupBy(_.projectId).map {
          case (id, row) => row.map(_.importTime).min
        }.firstOption
        Logger.info("time is " + minTime)

        minTime match {
          case Some(time) => {
            qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.clusterName === clusterName && tq.importTime === time).firstOption
          }
          case _ => {
            None
          }
        }
      }
      case _ => {
        val minTime = qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.clusterName.?.isEmpty).groupBy(_.projectId).map {
          case (id, row) => row.map(_.importTime).min
        }.firstOption
        Logger.info("time is " + minTime)

        minTime match {
          case Some(time) => {
            qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.importTime === time && tq.clusterName.?.isEmpty).firstOption
          }
          case _ => {
            None
          }
        }
      }
    }

//    if(minTime != None){
//      qTaskQueue.filter(_.envId === envId).filter(_.projectId === projectId).filter(_.importTime === minTime.get).first
//    }
//    else {
//      null
//    }
  }

  def findWaitQueueById(qId: Int): Option[TaskQueue] = db withSession{ implicit session =>
    qTaskQueue.filter(tq => tq.id === qId && tq.status === TaskEnum.TaskWait).firstOption
  }
  def findQueueNum(envId: Int, projectId: Int): Int = db withSession {implicit session =>
    //    Query(qTaskQueue.filter(_.envId is tq.envId).filter(_.projectId is tq.projectId).filter(_.status is TaskEnum.TaskWait).length).first
    qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.status === TaskEnum.TaskWait).length.run
  }

  def findQueues(envId: Int, projectId: Int, clusterName: Option[String]): List[TaskQueue] = db withSession { implicit session =>
    clusterName match {
      case Some(c) => {
        qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId && tq.clusterName === clusterName).list
      }
      case _ => {
        qTaskQueue.filter(tq => tq.envId === envId && tq.projectId === projectId).list
      }
    }

  }

  def findEnvId_ProjectId(): Set[(Int, Int, Option[String])] = db withSession { implicit session =>
    var set = Set.empty[(Int, Int, Option[String])]
    qTaskQueue.list.map{
      q =>
        set = set + ((q.envId, q.projectId, q.clusterName))
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
    qTaskQueue.filter(_.id === tq.id).delete
  }

  def deleteById(tqId: Int): Int = db withSession{implicit session =>
    Logger.info("remove from taskQueue")
    qTaskQueue.filter(_.id === tqId).delete
  }

  def update(tq: TaskQueue, taskId: Int) = db withSession{ implicit session =>
    qTaskQueue.filter(_.id === tq.id).update(tq.copy(status = TaskEnum.TaskProcess).copy(taskId = Option(taskId)))
  }

}