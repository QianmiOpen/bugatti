package controllers.task

import actor.ActorUtils
import actor.task.{TaskLog, MyActor}
import controllers.BaseController
import enums.TaskEnum
import org.joda.time.DateTime
import play.api.mvc.{Action, Controller}
import models.task._
import play.api.libs.json._
import utils.DateFormatter._
import play.api.Logger
import models.conf._
import play.api.mvc._
import utils.TaskTools
import sys.process._
import scala.io.Source
import scala.collection.{mutable, Seq}
import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Properties
import java.util.concurrent.{Executors, ExecutorService}
import play.api.libs.json.JsObject
import models.conf.Attribute
import models.conf.Environment
import models.conf.Project
import play.api.libs.json.JsObject

import play.api.mvc.{JavascriptLitteral, QueryStringBindable}

/**
 * 任务管理
 */
object TaskController extends BaseController {

  val stdFileDir = "/srv/salt"

  val templateDir = "/srv/sls"

  /**
   * 根据项目id获取最近的5个版本号，按照时间倒序
   * 在线上环境会过滤掉SNAPSHOT版本号
   * @param projectId
   * @param envId
   * @return
   */
  def getVersions(projectId: Int, envId: Int) = Action{
    val list = VersionHelper.findByProjectId_EnvId(projectId, envId)
    Ok(Json.toJson(list.reverse.drop(list.length - 5).reverse))
  }

  def findLastTaskStatus = Action(parse.json){implicit request =>
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
        TaskTemplateHelper.findById(t.taskTemplateId) match {
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
        TaskTemplateHelper.findById(t.taskTemplateId) match {
          case template => {
            tJson = tJson ++ Json.obj("taskName" -> template.name)
          }
        }
        Logger.debug(s"findStatus ==> ${tJson}")
        tJson
      }
    }
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
    val taskQueue = TaskQueue(None, envId, projectId, clusterName, versionId, templateId, TaskEnum.TaskWait, new DateTime, None, None, jobNo)
    val taskQueueId = TaskQueueHelper.create(taskQueue)
    MyActor.createNewTask(envId, projectId, clusterName)
    //test
//    var seq = Seq.empty[TaskQueue]
//    val doEnv = 2
//    val doPro = 2
//    for(i <- 1 to doEnv){
//      for(j <- 1 to doPro){
//        seq = seq :+ taskQueue.copy(envId = i).copy(projectId = j)
//      }
//    }
//    Logger.info(s"seq ==> $seq")
//    seq.foreach{
//      s =>{
//        TaskQueueHelper.create(s)
//        EnvironmentProjectRelHelper.create(EnvironmentProjectRel(None, Option(s.envId), Option(s.projectId), "t-syndic", "d6a597315b01", "172.19.3.134"))
//        MyActor.createNewTask(s.envId, s.projectId)
//      }
//    }
    taskQueueId
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
        //2、调用方法checkQueueNum修改状态；
//        TaskProcess.checkQueueNum(tq.envId, tq.projectId)
        //3、调用推送状态方法；
        //TODO TaskProcess.pushStatus()
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
    var map = Map.empty[Int, Seq[TaskTemplate]]
    var sVersion = Option(ScriptVersionHelper.Master)
    Logger.debug(s"scriptVersion ==> ${scriptVersion}")
    if(scriptVersion == ScriptVersionHelper.Latest){
      sVersion = ScriptVersionHelper.findLatest()
    }
    Logger.debug(s"sVersion ==> ${sVersion}")
    sVersion match {
      case Some(sv) => {
        TaskTemplateHelper.findByScriptVerison(sv).foreach{
          template => {
            //1、从map中获取seq，没有就创建
            var seq = map.getOrElse(template.typeId, Seq.empty[TaskTemplate])
            //2、添加到seq
            seq = seq :+ template
            //3、覆盖map
            map += template.typeId -> seq
          }
        }
      }
      case _ => {
        //模板返回空
      }
    }
    Logger.debug(s"templates ==> ${map}")
    Ok(map2Json(map))
  }

  def map2Json(map: Map[Int, Seq[TaskTemplate]]): JsValue = {
    var result = Json.obj()
    map.foreach{
      m => {
        result ++= Json.obj(s"${m._1}" -> m._2)
      }
    }
    Json.toJson(result)
  }

  def taskLog(taskId: Int) = WebSocket.async[JsValue] { request =>
    TaskLog.show(taskId)
  }

  def taskLogFirst(taskId: Int, byteSize: Int) = Action {
    Logger.info("taskId:"+taskId+",byteSize:"+byteSize)
    TaskLog.readHeader(taskId, byteSize)
    Ok
  }

  def forceTerminate(envId: Int, projectId: Int, clusterName: Option[String]) = Action {
    MyActor.forceTerminate(envId, projectId, clusterName)
    Ok
  }

  //=======================任务界面重构===========================================
  def findClusterByEnv_Project(envId: Int, projectId: Int) = Action {
    Ok(Json.toJson(EnvironmentProjectRelHelper.findByEnvId_ProjectId(envId, projectId)))
  }

  implicit val varWrites = Json.writes[Variable]
  implicit val projectWrites = Json.writes[Project]
  implicit val taskWrites = Json.writes[Task]
  implicit val versionWrites = Json.writes[Version]
  implicit val taskTemplateWrites = Json.writes[TaskTemplate]
  implicit val envProRelWrites = Json.writes[EnvironmentProjectRel]

}
