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
case class Task(id: Option[Int], envId: Int, projectId: Int, versionId: Option[Int], taskTemplateId:Int, status: TaskStatus, startTime: Option[DateTime], endTime: Option[DateTime], operatorId: Int)

case class TaskTable(tag: Tag) extends Table[Task](tag, "task") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.NotNull)
  def projectId = column[Int]("project_id", O.NotNull)
  def versionId = column[Int]("version_id", O.Nullable)
  def taskTemplateId = column[Int]("task_template_id",O.NotNull)
  def status = column[TaskStatus]("status", O.NotNull)
  def startTime = column[DateTime]("start_time", O.Nullable, O.DBType("DATETIME"))
  def endTime = column[DateTime]("end_time", O.Nullable, O.DBType("DATETIME"))
  def operatorId = column[Int]("operator_id", O.NotNull)

  override def * = (id.?, envId, projectId, versionId.?, taskTemplateId, status, startTime.? ,endTime.?, operatorId) <> (Task.tupled, Task.unapply _)
}

object TaskHelper {

  implicit val taskReads: Reads[Task] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "envId").read[Int] and
    (JsPath \ "projectId").read[Int] and
    (JsPath \ "versionId").readNullable[Int] and
    (JsPath \ "taskTemplateId").read[Int] and
    (JsPath \ "status").read[TaskStatus] and
    (JsPath \ "startTime").readNullable[DateTime] and
    (JsPath \ "endTime").readNullable[DateTime] and
    (JsPath \ "operatorId").read[Int]
  )(Task.apply _)

  import models.AppDB._

  val qTask = TableQuery[TaskTable]
  val qEnvironment = TableQuery[EnvironmentTable]
  val qProject = TableQuery[ProjectTable]
  val qTaskAttribute = TableQuery[AttributeTable]
  val qTaskQueue = TableQuery[TaskQueueTable]


  def findLastStatus(envId: Int, projects: JsValue): List[Task] = db withSession { implicit session =>
    val list: List[Int] = (projects \\ "id").toList.map( id => id.toString.toInt)

    val maxEndTime = qTask.where(_.envId is envId).where(_.projectId inSet list.toSeq).groupBy(_.projectId)
      .map {
        case (projectId, row) => projectId -> row.map(_.startTime).max
      }

    val taskQuery = for{
      task <- qTask
      max <- maxEndTime
      if(task.projectId === max._1 && task.startTime === max._2)
    }yield task
    taskQuery.list
  }

  def findLastStatusByProject(envId: Int, projectId: Int): List[Task] = {
    val projects = Json.obj("id" -> projectId)
    findLastStatus(envId, projects)
  }

  implicit def taskQueue2Task(tq: TaskQueue): Task ={
    Task(None, tq.envId, tq.projectId, tq.versionId, tq.taskTemplateId, enums.TaskEnum.TaskProcess, Option(new DateTime()), None, 1)
  }

  def addByTaskQueue(tq: TaskQueue): Int = db withSession { implicit session =>
    //2、insert Task
    val taskId = add(tq)
    //3、update taskQueue
    TaskQueueHelper.updateStatus(tq, taskId)
    taskId
  }

  def changeStatus(taskId: Int, status: TaskStatus) = db withSession { implicit session =>
    val task = qTask.where(_.id === taskId).first
    val taskUpdate = task.copy(status = status, endTime = Some(new DateTime()))
    qTask.where(_.id === taskId).update(taskUpdate)
  }

  def count(envId: Int, projectId: Int): Int = db withSession { implicit session =>
    (envId, projectId) match {
      case (e, p) if e != -1 && p != -1 =>{
        Query(qTask.where(_.envId is envId).where(_.projectId is projectId).length).first
      }
      case (e, p) if e == -1 && p != -1 =>{
        Query(qTask.where(_.projectId is projectId).length).first
      }
      case (e, p) if e != -1 && p == -1 =>{
        Query(qTask.where(_.envId is envId).length).first
      }
      case (e, p) if e == -1 && p == -1 =>{
        Query(qTask.length).first
      }
    }
  }

  def addByJson(json: JsValue) = db withSession{ implicit session =>
    val taskResult:JsResult[Task] = json.validate[Task]
    Logger.info(taskResult.toString)
    taskResult match {
      case s : JsSuccess[Task] => add(s.get)
      case e : JsError => {
        Logger.info("Errors: " + JsError.toFlatJson(e).toString())
        0
      }
    }
  }

  def add(task: Task) = db withSession { implicit session =>
    val tasktmp = task.copy(startTime = Some(new DateTime()))
    val taskId = qTask.returning(qTask.map(_.id)).insert(tasktmp)
    Logger.info("task db ==> "+taskId)
    taskId
  }

  def all(page: Int, pageSize: Int) = db withSession { implicit session =>
    val offset = pageSize * page
    val query = (for {
      ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId is _.id) innerJoin qProject on (_._1.projectId is _.id)
    } yield (task,environment,project)).sortBy(s => s._1.startTime.desc)
    query.drop(offset).take(pageSize).list
  }

  def findTask(taskId: Int) = db withSession { implicit session =>
    val task = qTask.where(_.id is taskId).first
    task
  }

  def findByEnv(envId: Int, projectId: Int, page: Int, pageSize: Int) = db withSession { implicit session =>
    val offset = pageSize * page
    (envId, projectId) match {
      case (e, p) if e != -1 && p != -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId is _.id) innerJoin qProject on (_._1.projectId is _.id)
          if task.envId is envId
          if task.projectId is projectId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case (e, p) if e != -1 && p == -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId is _.id) innerJoin qProject on (_._1.projectId is _.id)
          if task.envId is envId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case (e, p) if e == -1 && p != -1 => {
        (for {
          ((task, environment),project) <- qTask innerJoin  qEnvironment on (_.envId is _.id) innerJoin qProject on (_._1.projectId is _.id)
          if task.projectId is projectId
        } yield (task,environment,project)).sortBy(s => s._1.startTime.desc).drop(offset).take(pageSize).list
      }
      case _ => all(page, pageSize)
    }
  }
}
