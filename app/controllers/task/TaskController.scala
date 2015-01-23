package controllers.task

import actor.task.{ChangeQueues, TaskLog, MyActor}
import controllers.BaseController
import enums.TaskEnum
import org.joda.time.DateTime
import play.api.mvc.Action
import models.task._
import play.api.libs.json._
import utils.DateFormatter._
import play.api.Logger
import models.conf._
import play.api.mvc._
import scala.collection.Seq
import models.conf.Project
import play.api.libs.json.JsObject

/**
 * 任务管理
 */
object TaskController extends BaseController {

  val stdFileDir = "/srv/salt"

  val templateDir = "/srv/sls"

  implicit val varWrites = Json.writes[Variable]
  implicit val projectWrites = Json.writes[Project]
  implicit val taskWrites = Json.writes[Task]
  implicit val taskTemplateWrites = Json.writes[TemplateAction]
  implicit val envProRelWrites = Json.writes[Host]

  def findLastTaskStatus = Action(parse.json){ implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.toJson(findStatus(fields)))
      }
      case _ => Ok(Json.toJson(0))
    }
  }

  def findLastStatus(envId: Int, projectId: Int, clusters: String) = Action{
    val list = TaskHelper.findLastStatusByClusters(envId, projectId, clusters.split(",")).map{
      t => {
        var tJson = Json.toJson(t).as[JsObject]
        VersionHelper.findById(t.versionId.getOrElse(0)) match {
          case Some(version) => {
            tJson = tJson ++ Json.obj("version" -> version.vs)
          }
          case _ => {}
        }
        TemplateActionHelper.findById(t.taskTemplateId) match {
          case template => {
            tJson = tJson ++ Json.obj("taskName" -> template.name)
          }
        }
        Logger.debug(s"findStatus ==> ${tJson}")
        tJson
      }
    }
    Ok(Json.toJson(list))
  }

  def findStatus(fields: Seq[(String, JsValue)]): List[JsValue] = {
    val jsons = Json.toJson(fields.toMap)
    val envId = (jsons \ "envId").toString.toInt
    val projects = (jsons \ "projects")
    Logger.debug(s"${projects}")
//    projects.as[JsArray].value
    TaskHelper.findLastStatus(envId, projects).map{
      t => {
        Logger.debug(s"t ==> ${t.taskTemplateId}, ${t.versionId}")
        var tJson = Json.toJson(t).as[JsObject]
        VersionHelper.findById(t.versionId.getOrElse(0)) match {
          case Some(version) => {
            tJson = tJson ++ Json.obj("version" -> version.vs)
          }
          case _ => {}
        }
        TemplateActionHelper.findById(t.taskTemplateId) match {
          case template => {
            tJson = tJson ++ Json.obj("taskName" -> template.name)
          }
        }
        Logger.debug(s"findStatus ==> ${tJson}")
        tJson
      }
    }
  }

  def findHisTasks(envId: Int, projectId: Int)= Action {
    val list = TaskHelper.findHisTasks(envId, projectId)
    val result = list.map{
      t =>
          Logger.debug(s"history => ${t._1}, ${t._2}, ${t._3}")
        Json.toJson(t._1).as[JsObject] ++ Json.obj("vs" -> t._2) ++ Json.obj("desc" -> t._3.name)
    }
    Ok(Json.toJson(result))
  }

  def joinProcess(taskId: Int) = WebSocket.async[JsValue] { request =>
    MyActor.join()
  }

  def createNewTaskQueue = Action(parse.json) {implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.toJson(addTaskQueue(fields)))
      }
      case _ => Ok(Json.toJson(0))
    }
  }

  def addTaskQueue(fields: Seq[(String, JsValue)]): Int = {
    val fieldsJson = Json.toJson(fields.toMap)
    val tq = fieldsJson \ "taskQueue"
    val envId = (tq \ "envId").as[Int]
    val projectId = (tq \ "projectId").as[Int]
    val clusterName = (tq \ "clusterName").asOpt[String]
    val versionId = (tq \ "versionId").asOpt[Int]
    Logger.info(s"version ==> ${versionId}")

    val templateId = (tq \ "templateId").as[Int]
    val jobNo = (tq \ "operatorId").as[String]

    //check the templateId is real
    val esv = EnvironmentHelper.findById(envId).get.scriptVersion
    val csv = TemplateActionHelper.findById(templateId).scriptVersion
    if(esv != csv){
      -1
    }else {
      val taskQueue = TaskQueue(None, envId, projectId, clusterName, versionId, templateId, TaskEnum.TaskWait, new DateTime, None, None, jobNo)
      val taskQueueId = TaskQueueHelper.create(taskQueue)
      MyActor.createNewTask(envId, projectId, clusterName)
      taskQueueId
    }
  }

  /**
   * 从队列中删除正在等待执行的任务
   * @param qid
   * @return
   */
  def removeTaskQueue(qid: Int) = Action{
    val taskQueue = TaskQueueHelper.findWaitQueueById(qid)
    taskQueue match {
      case Some(tq) => {
        //1、删除队列；
        TaskQueueHelper.delete(tq)
        //2、调用Actor修改Queue & QueueNum状态；
        MyActor.superviseTaskActor ! ChangeQueues(tq.envId, tq.projectId, tq.clusterName)
      }
      case _ => {

      }
    }
    Ok
  }

  /**
   * 获取所有项目类型的模板
   */
  def getTemplates(scriptVersion: String) = Action{
    var map = Map.empty[Int, Seq[TemplateAction]]
    Logger.debug(s"scriptVersion ==> ${scriptVersion}")

    TemplateActionHelper.findByScriptVerison(scriptVersion).foreach{
      template => {
        //1、从map中获取seq，没有就创建
        var seq = map.getOrElse(template.typeId, Seq.empty[TemplateAction])
        //2、添加到seq
        seq = seq :+ template
        //3、覆盖map
        map += template.typeId -> seq
      }
    }
    Logger.debug(s"templates ==> ${map}")
    Ok(map2Json(map))
  }

  def map2Json(map: Map[Int, Seq[TemplateAction]]): JsValue = {
    var result = Json.obj()
    map.foreach{
      m => {
        result ++= Json.obj(s"${m._1}" -> m._2)
      }
    }
    Json.toJson(result)
  }

  def logReader(taskId: Int) = Action{
    val (logHeader, logContent) = TaskLog.readLog(taskId)
    val result = Json.obj("logHeader" -> logHeader, "logContent" -> logContent)
    Ok(result)
  }

  def logHeaderContent(taskId: Int, byteSize: Int) = Action {
    Ok(TaskLog.readHeader(taskId, byteSize))
  }

  def forceTerminate(envId: Int, projectId: Int, clusterName: Option[String]) = Action {
    MyActor.forceTerminate(envId, projectId, clusterName)
    Ok
  }

  //=======================任务界面重构===========================================
  def findClusterByEnv_Project(envId: Int, projectId: Int) = Action {
    Ok(Json.toJson(HostHelper.findByEnvId_ProjectId(envId, projectId)))
  }

  def findCatalinaWSUrl(envId: Int) = Action{
    var ip = "172.19.0.0"
    TemplateHelper.findByName("logstash") match {
      case Some(template) =>
        val projects = ProjectHelper.allByTemplateId(template.id.get)
        if(!projects.isEmpty){
          val rels = HostHelper.findByEnvId_ProjectId(envId, projects(0).id.get)
          if(!rels.isEmpty){
            ip = rels(0).ip
          }else {
            Logger.error("获取不到logstash机器")
          }
        }else {
          Logger.error("获取不到logstash项目")
        }
      case _ =>
        Logger.error("获取不到logstash模板")
    }
    Ok(s"ws://${ip}:3232")
  }

}
