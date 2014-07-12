package controllers.task

import controllers.actor.{TaskLog, TaskProcess}
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

//import scala.Seq

/**
 * 任务管理
 */
object TaskController extends Controller {

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
    val list = VersionHelper.findByPid_Eid(projectId, envId)
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

  def findStatus(fields: Seq[(String, JsValue)]): List[JsValue] = {
    val jsons = Json.toJson(fields.toMap)
    val envId = (jsons \ "envId").toString.toInt
    val projects = (jsons \ "projects")
    Logger.info(""+projects(0))
    projects.as[JsArray].value
    TaskHelper.findLastStatus(envId, projects).map{
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
        tJson
      }
    }
  }

  def joinProcess(taskId: Int) = WebSocket.async[JsValue] { request =>
    TaskProcess.join()
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
    val versionId = (tq \ "versionId").asOpt[Int]
    Logger.info(s"version ==> ${versionId}")
    val templateId = (tq \ "templateId").as[Int]
    val taskQueue = TaskQueue(None, envId, projectId, versionId, templateId, TaskEnum.TaskWait, new DateTime, None, None, 1)
    TaskProcess.createNewTask(taskQueue)
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
        TaskProcess.checkQueueNum(tq.envId, tq.projectId)
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
  def getTemplates() = Action{
    var map = Map.empty[Int, Seq[TaskTemplate]]
    TaskTemplateHelper.all.foreach{
      template => {
        //1、从map中获取seq，没有就创建
        var seq = map.getOrElse(template.typeId, Seq.empty[TaskTemplate])
        //2、添加到seq
        seq = seq :+ template
        //3、覆盖map
        map += template.typeId -> seq
      }
    }
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
    TaskLog.show(request.session.hashCode().toString, taskId)
  }

  def taskLogFirst(taskId: Int, byteSize: Int) = WebSocket.using[String] { request =>
    Logger.info("taskId:"+taskId+",byteSize:"+byteSize)
    TaskLog.readHeader(taskId, byteSize)
  }

  implicit val projectWrites = Json.writes[Project]
  implicit val taskWrites = Json.writes[Task]
  implicit val versionWrites = Json.writes[Version]
  implicit val taskTemplateWrites = Json.writes[TaskTemplate]

}
