package models.task

import enums.TaskEnum._

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
import org.joda.time._
import com.github.tototoshi.slick.MySQLJodaSupport._
import models.conf._
import play.api.libs.json._

import play.api.libs.functional.syntax._
import play.api.Logger
import play.api.libs.json.JsSuccess

/**
 * 任务
 * @author of729
 * @author of546
 */
case class Task(id: Option[Int], envId: Int, projectId: Int, clusterName: Option[String], versionId: Option[Int], taskTemplateId:Int, status: TaskStatus, startTime: Option[DateTime], endTime: Option[DateTime], operatorId: String)

case class TaskTable(tag: Tag) extends Table[Task](tag, "task") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def projectId = column[Int]("project_id")
  def clusterName = column[String]("cluster_name", O.Nullable)
  def versionId = column[Int]("version_id", O.Nullable)
  def taskTemplateId = column[Int]("task_template_id")
  def status = column[TaskStatus]("status")
  def startTime = column[DateTime]("start_time", O.Nullable, O.DBType("DATETIME"))
  def endTime = column[DateTime]("end_time", O.Nullable, O.DBType("DATETIME"))
  def operatorId = column[String]("operator_id")

  override def * = (id.?, envId, projectId, clusterName.?, versionId.?, taskTemplateId, status, startTime.? ,endTime.?, operatorId) <> (Task.tupled, Task.unapply _)
}

object TaskHelper {

  implicit val taskReads: Reads[Task] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "envId").read[Int] and
    (JsPath \ "projectId").read[Int] and
    (JsPath \ "clusterName").readNullable[String] and
    (JsPath \ "versionId").readNullable[Int] and
    (JsPath \ "taskTemplateId").read[Int] and
    (JsPath \ "status").read[TaskStatus] and
    (JsPath \ "startTime").readNullable[DateTime] and
    (JsPath \ "endTime").readNullable[DateTime] and
    (JsPath \ "operatorId").read[String]
  )(Task.apply _)

  import models.AppDB._

  val qTask = TableQuery[TaskTable]
  val qEnvironment = TableQuery[EnvironmentTable]
  val qProject = TableQuery[ProjectTable]
  val qTaskAttribute = TableQuery[AttributeTable]
  val qTaskQueue = TableQuery[TaskQueueTable]
  val qVersion = TableQuery[VersionTable]
  val qTaskTemplate = TableQuery[TaskTemplateTable]

  def findById(taskId: Int) = db withSession { implicit session =>
    val task = qTask.filter(_.id === taskId).first
    task
  }

  def findHisTasks(envId: Int, projectId: Int): List[(Task, Option[String], TaskTemplate)] = db withSession { implicit session =>
//    (for{
//      ((task, template), version) <- qTask innerJoin qTaskTemplate on (_.taskTemplateId === _.id) leftJoin qVersion on (_._1.versionId === _.id)
//      if task.envId is envId
//      if task.projectId is projectId
//    } yield {
//      (task, version.vs.?, template)
//    }).sortBy(s => s._1.startTime.desc).take(20).list

    val list = (for{
      (task, template) <- qTask innerJoin qTaskTemplate on (_.taskTemplateId === _.id)
      if(task.envId === envId && task.projectId === projectId)
    } yield {
      (task, template)
    }).sortBy(s => s._1.startTime.desc).take(20).list
    val versionMap = qVersion.filter(_.projectId === projectId).list.map(t => t.id -> t.vs).toMap[Option[Int], String]
    list.map{
      t =>
        (t._1, versionMap.get(t._1.versionId), t._2)
    }
  }

  def all(page: Int, pageSize: Int) = db withSession { implicit session =>
    val offset = pageSize * page
    val query = (for {
      ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId === _.id) innerJoin qProject on (_._1.projectId === _.id)
    } yield (task,environment,project)).sortBy(s => s._1.startTime.desc)
    query.drop(offset).take(pageSize).list
  }

  def count(envId: Int, projectId: Int): Int = db withSession { implicit session =>
    (envId, projectId) match {
      case (e, p) if e != -1 && p != -1 => {
        qTask.filter(t => t.envId === envId && t.projectId === projectId).length.run
      }
      case (e, p) if e == -1 && p != -1 => {
        qTask.filter(_.projectId === projectId).length.run
      }
      case (e, p) if e != -1 && p == -1 => {
        qTask.filter(_.envId === envId).length.run
      }
      case (e, p) if e == -1 && p == -1 => {
        qTask.length.run
      }
    }
  }

  def findByEnv(envId: Int, projectId: Int, page: Int, pageSize: Int) = db withSession { implicit session =>
    val offset = pageSize * page
    (envId, projectId) match {
      case (e, p) if e != -1 && p != -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId === _.id) innerJoin qProject on (_._1.projectId === _.id)
          if task.envId === envId
          if task.projectId === projectId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case (e, p) if e != -1 && p == -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId === _.id) innerJoin qProject on (_._1.projectId === _.id)
          if task.envId === envId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case (e, p) if e == -1 && p != -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId === _.id) innerJoin qProject on (_._1.projectId === _.id)
          if task.projectId === projectId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case _ => all(page, pageSize)
    }
  }

  def findLastStatus(envId: Int, projects: JsValue): List[Task] = db withSession { implicit session =>
    val list: List[Int] = (projects \\ "id").toList.map( id => id.toString.toInt)

    val maxEndTime = qTask.filter(t => t.envId === envId && (t.projectId inSet list.toSeq)).groupBy(_.projectId)
      .map {
      case (projectId, row) => projectId -> row.map(_.startTime).max
    }

    val taskQuery = for{
      task <- qTask
      max <- maxEndTime
      if (task.projectId === max._1 && task.startTime === max._2)
    } yield task
    taskQuery.list
  }

  def findLastStatusByClusters(envId: Int, projectId: Int, clusters: Seq[String]): Seq[Task] = db withSession { implicit session =>
//    val projects = Json.obj("id" -> projectId)
//    findLastStatus(envId, projects)
    val maxEndTime = qTask.filter(t => t.envId === envId && t.projectId === projectId && (t.clusterName inSet clusters) ).groupBy(_.clusterName)
      .map {
      case (clusterName, row) => clusterName -> row.map(_.startTime).max
    }
    val taskQuery = for{
      task <- qTask
      max <- maxEndTime
      if (task.clusterName === max._1 && task.startTime === max._2)
    } yield task
    taskQuery.list
  }

  implicit def taskQueue2Task(tq: TaskQueue): Task ={
    Task(None, tq.envId, tq.projectId, tq.clusterName, tq.versionId, tq.taskTemplateId, enums.TaskEnum.TaskProcess, Option(new DateTime()), None, tq.operatorId)
  }

  def addByTaskQueue(tq: TaskQueue): Int = db withSession { implicit session =>
    //2、insert Task
    val taskId = create(tq)
    //3、update taskQueue
    TaskQueueHelper.update(tq, taskId)
    taskId
  }

  def changeStatus(taskId: Int, status: TaskStatus) = db withSession { implicit session =>
    val task = qTask.filter(_.id === taskId).first
    val taskUpdate = task.copy(status = status, endTime = Some(new DateTime()))
    qTask.filter(_.id === taskId).update(taskUpdate)
  }

  def addByJson(json: JsValue) = db withSession{ implicit session =>
    val taskResult:JsResult[Task] = json.validate[Task]
    Logger.info(taskResult.toString)
    taskResult match {
      case s : JsSuccess[Task] => create(s.get)
      case e : JsError => {
        Logger.info("Errors: " + JsError.toFlatJson(e).toString())
        0
      }
    }
  }

  def create(task: Task) = db withSession { implicit session =>
    val task2create = task.copy(startTime = Some(new DateTime()))
    val taskId = qTask.returning(qTask.map(_.id)).insert(task2create)
    Logger.info("task db ==> "+taskId)
    taskId
  }

}
