package controllers.task

import org.joda.time.DateTime
import play.api.mvc.{Action, Controller}
import models.task._
import play.api.libs.json._
import utils.DateFormatter._
import play.api.Logger
import models.conf._
import play.api.mvc._
import sys.process._
import scala.io.Source
import scala.collection.{mutable, Seq}
import java.io._
import java.net.{HttpURLConnection, URL}
import java.util.Properties
import java.util.concurrent.{Executors, ExecutorService}
import models.task.Task
import play.api.libs.json.JsObject
import models.conf.Attribute
import models.conf.Environment
import models.task.Task
import models.conf.Project
import play.api.libs.json.JsObject

//import scala.Seq

/**
 * 任务管理
 */
object TaskController extends Controller {

  val stdFileDir = "/srv/salt"

  val templateDir = "/srv/sls"

  val httpURL = "http://nexus.dev.ofpay.com/nexus/content/repositories/releases/"

  val httpURLSNAP = "http://nexus.dev.ofpay.com/nexus/content/repositories/snapshots/"

  /**
   * 根据项目id获取最近的5个版本号，按照时间倒序
   * 在线上环境会过滤掉SNAPSHOT版本号
   * @param projectId
   * @param envId
   * @return
   */
  def getVersions(projectId: Int, envId: Int) = Action{
    val list = VersionHelper.findByPidAndEid(projectId, envId)
    Ok(Json.toJson(list.reverse.drop(list.length - 5).reverse))
  }

  def findVersionsByProjects = Action(parse.json){implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.toJson(findVersions(fields)))
      }
      case _ => Ok(Json.toJson(0))
    }
  }

  def findVersions(fields: Seq[(String, JsValue)]): List[Project] = {
    null
  }

  def lastVersions(fields: Seq[(String, JsValue)], isSnapshot: Boolean, num: Int) ={
    val projects: JsArray = Json.arr(Json.toJson(fields.toMap) \ "projects")
    for(fieldsJson <- projects.value){
      val groupName = (fieldsJson \ "groupId").toString.replaceAll("\\.", "/").replace("\"","")
      val projectName = (fieldsJson \ "artifactId").toString.replaceAll("\\.", "/").replace("\"","")
      var list = mutable.LinkedList[String]()
      var url = httpURL +groupName + File.separator + projectName
      if(isSnapshot){
        url = httpURLSNAP +groupName + File.separator + projectName
      }
      try{
        val source = Source.fromURL(url)
        val htmlSource = source.mkString
        val reg = """<a href=".+">([^/]+)/</a>""".r
        val regMatchs = reg.findAllMatchIn(htmlSource)
        for(regMatch <- regMatchs){
          list = list :+ regMatch.group(1).toString
        }
        source.close
      }catch{
        case ex: Exception => Logger.error("version error : " + ex.toString)
      }
      Logger.info("versions==>" + list)
      list.toList
    }
  }

  def findLastTaskStatus = Action(parse.json){implicit request =>
    request.body match {
      case JsObject(fields) => {
        Ok(Json.toJson(findStatus(fields)))
      }
      case _ => Ok(Json.toJson(0))
    }
  }

  def findStatus(fields: Seq[(String, JsValue)]): List[Task] = {
    val jsons = Json.toJson(fields.toMap)
    val envId = (jsons \ "envId").toString.toInt
    val projects = (jsons \ "projects")
    Logger.info(""+projects(0))
    projects.as[JsArray].value
    TaskHelper.findLastStatus(envId, projects)
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
    val envId = (tq \ "envId").toString.toInt
    val projectId = (tq \ "projectId").toString.toInt
    val version = (tq \ "version").toString
    val templateId = (tq \ "templateId").toString.toInt
    val taskQueue = TaskQueue(None, envId, projectId, version, templateId, 0, new DateTime, None, 1)
    TaskProcess.createNewTask(taskQueue)
  }

  implicit val projectWrites = Json.writes[Project]
  implicit val taskWrites = Json.writes[Task]
  implicit val versionWrites = Json.writes[Version]

}
