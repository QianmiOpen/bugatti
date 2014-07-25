package controllers.actor

import java.io.File

import actor.ActorUtils
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import enums.TaskEnum
import models.task.{Task, TaskHelper}
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.iteratee._
import play.api.libs.json.{JsObject, JsString, _}
import utils.{ConfHelp, Reader}

import scala.concurrent.duration._
//import models.configure.ProjectHelper

/**
 * Created by jinwei on 8/7/14.
 */

object TaskLog{

  implicit val timeout = Timeout(2 seconds)

//  val stdFileDir = "/srv/salt"

  val baseLogPath = ConfHelp.logPath

  lazy val system = ActorUtils.system

  var actorsMap = {
    Map.empty[Int, ActorRef]
  }

  var scheduleMap = {
    Map.empty[Int, Cancellable]
  }

  def chooseSystem(taskId: Int): ActorRef = {
    actorsMap get taskId getOrElse {
      val actor = system actorOf Props[TaskLog]
      actorsMap += taskId -> actor
      actor
    }
  }

  def startSchedule(taskId: Int){
    scheduleMap get taskId getOrElse{
      val actor = actorsMap.get(taskId).get
      val schedule = new TaskSchedule
      scheduleMap += taskId -> schedule.start(actor, taskId)
      scheduleMap.get(taskId).get
    }
  }

  def show(sessionId: String, taskId: Int): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    val actor = chooseSystem(taskId)
    val schedule = startSchedule(taskId)
    val task = TaskHelper.findById(taskId)
//    val task = null
    val logFilePath = getLogFilePath(taskId)
//    val logFilePath = "/Users/jinwei/tmp/1564.log"
//    Logger.info("actor==>"+actor.hashCode().toString)
//    Logger.info("actor==>"+actor.hashCode().toString)
//    Logger.info("schedule==>"+scheduleMap.get(taskId).hashCode().toString)
    (actor ? Join(taskId, task, logFilePath)).map {
      case Connected(enumerator, text) =>{
        //        actor ! logFirst(text)
        val iteratee = Iteratee.foreach[JsValue](println).map{ _ =>
          (actor ? Quit(taskId)).map {
            case CancelSchedule(enumerator, taskId) => {
              //              val iteratee = Iteratee.foreach[JsValue]{println}.map{ _ =>}
              scheduleMap.get(taskId).get.cancel
              scheduleMap -= taskId
              actorsMap -= taskId
              //              (iteratee,enumerator)
            }
          }
        }
        (iteratee,enumerator)
      }

      case CannotConnect(error) =>{
        val iteratee = Done[JsValue,Unit]((),Input.EOF)
        // Send an error and close the socket
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee,enumerator)
      }

    }
  }

  def getLogFilePath(taskId: Int): String = {
    val filePath = s"${baseLogPath}/${taskId}/result.log"
    new File(filePath).getAbsolutePath
//    val projectName = ProjectHelper.findById(task.projectId).get.name
//    val path = stdFileDir + File.separator + "jobs" + File.separator + projectName + File.separator + taskId + File.separator + "execute.log"
//    path
    //for test
//        "/Users/jinwei/jstack.log"
  }

  def readHeader(taskId: Int, byteSize: Int) = {
    val path = getLogFilePath(taskId)
    val file = new File(path)
    val reader = new Reader()
    val str = reader.reader(file, 0, byteSize)._1

    val in = Iteratee.consume[String]()
//    Logger.info("file head:"+ str)
    val out = Enumerator(str).andThen(Enumerator.eof)
    (in, out)
  }
}
class TaskLog extends Actor{
  var count = 0
  var members = Set.empty[String]
  val (logEnumerator, logChannel) = Concurrent.broadcast[JsValue]

  var from = 0L
  var fromFlag = from
  val offset = 4*1024L
  val reader = new Reader()

  var logHeaderText = ""

  var isOk = false

  var path = ""

  def receive = {
    case Join(taskId, task, logFilePath) => {
      //判断task是否已经执行完成，如果执行成功，则只读取1024个字节，其余隐藏；若总字节数不足1024，则全部读取
      count = count + 1
      if(task.status != TaskEnum.TaskProcess){//执行结束
        path = logFilePath
        val file = new File(path)
        from = file.length() - 1024L
        if(from < 0L){
          from = 0L
        }
        isOk = true
      }
//      Logger.info("Join=>"+count)
//      Logger.info("first from=>"+from)
      if(from > 0){
        logHeaderText = from + " bytes is hidden,show them?"
      }

      sender ! Connected(logEnumerator, logHeaderText)
//      Logger.info("sender==>"+sender.hashCode())
//      Logger.info("self==>"+self.hashCode())
    }

    case logFirst(text) => {
//      Logger.info(" why this message do not show : "+text)
      notifyAll("logFirst", from.toString, text)
      //      notifyAll("log", sessionId, text)
    }

    case Quit(taskId) => {
      count = count - 1
//      Logger.info("Quit=>"+count)
      //      members = members - session
      notifyAll("quit", "session", "has quit")
      //无连接时停止定时器
      if(count == 0){
        sender ! CancelSchedule(logEnumerator, taskId)
      }
    }

    case taskId: Int =>
      //根据任务找到file path
      val str = logReader(taskId)
      notifyAll("log", fromFlag.toString, str)
  }

  def logReader(taskId: Int): String = {
    fromFlag = from
    if(path != ""){
      val file = new File(path)
      val (bufferStr, len) = reader.reader(file, from, offset)
      if(len != -1){
        from = from + len.toLong
      }
//      Logger.info("from ====> " + from.toString)
      bufferStr.toString
    }else{
      ""
    }
  }

  def notifyAll(kind: String, session: String, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind)
        ,"from" -> JsString(session)
        ,"message" -> JsString(text)
      )
    )
    logChannel.push(msg)
    //隐藏的字节数
    if(logHeaderText != ""){
      val msgCover = JsObject(
        Seq(
          "kind" -> JsString("logFirst")
          ,"from" -> JsString(fromFlag.toString)
          ,"message" -> JsString(logHeaderText)
        )
      )
      logChannel.push(msgCover)
    }
  }
}

//object TaskLogFirst{
//  val stdFileDir = "/srv/salt"
//
//  def getLogFilePath(taskId: Int): String = {
////    val task = TaskHelper.findTask(taskId)
////    val projectName = ProjectHelper.findById(task.projectId).get.name
////    val path = stdFileDir + File.separator + "jobs" + File.separator + projectName + File.separator + taskId + File.separator + "execute.log"
////    path
//    //for test
//        "/Users/jinwei/jstack.log"
//  }
//
//  def readHeader(taskId: Int, byteSize: Int) = {
//    val path = getLogFilePath(taskId)
//    val file = new File(path)
//    val reader = new Reader()
//    val str = reader.reader(file, 0, byteSize)._1
//
//    val in = Iteratee.consume[String]()
//    val out = Enumerator(str).andThen(Enumerator.eof)
//    (in, out)
//  }
//}

case class Join(taskId: Int, task: Task, logFilePath: String)
case class Connected(enumerator: Enumerator[JsValue], text: String)
case class logFirst(text: String)
case class Quit(taskId: Int)
case class CancelSchedule(enumerator: Enumerator[JsValue], taskId: Int)

class TaskSchedule{
  def start(taskActor: ActorRef, taskId: Int): Cancellable = {
    Akka.system.scheduler.schedule(
      1 second,
      2 seconds,
      taskActor,
      taskId
    )
  }
}
