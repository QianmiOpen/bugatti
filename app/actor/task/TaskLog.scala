package actor.task

import java.io.File

import actor.ActorUtils
import akka.actor.{ActorRef, ActorLogging, Props, Actor}
import akka.util.Timeout
import akka.pattern.ask
import models.task.{Task, TaskHelper}
import play.api.Play.current
import play.api.libs.iteratee._
import play.api.libs.json.{JsString, JsObject, JsValue}
import play.api.libs.concurrent.Execution.Implicits._
import utils.{Reader, ConfHelp}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by jinwei on 5/8/14.
 */
object TaskLog{
  implicit val timeout = Timeout(2 seconds)

  val baseLogPath = ConfHelp.logPath

  lazy val system = ActorUtils.system

  lazy val taskLogWs = system.actorOf(Props[TaskLogWs], "taskLogWs")

  //建立websocket连接
  def show(taskId: Int): scala.concurrent.Future[(Iteratee[JsValue,_],Enumerator[JsValue])] = {
    (taskLogWs ? Join(taskId)).map{
      case Connected(out) => {
        val in = Iteratee.foreach[JsValue] {
          event =>
        }.map{_ => taskLogWs ! Quit(taskId)}
        (in, out)
      }
      case CannotConnect(error) =>{
        val iteratee = Done[JsValue,Unit]((),Input.EOF)
        // Send an error and close the socket
        val enumerator =  Enumerator[JsValue](JsObject(Seq("error" -> JsString(error)))).andThen(Enumerator.enumInput(Input.EOF))

        (iteratee,enumerator)
      }
    }
  }

  def readHeader(taskId: Int, byteSize: Int) = {

    taskLogWs ! ReadHeader(taskId, byteSize)
//    val path = getLogFilePath(taskId)
//    val file = new File(path)
//    val reader = new Reader()
//    val str = reader.reader(file, 0, byteSize)._1
//
//    val in = Iteratee.consume[String]()
//    //    Logger.info("file head:"+ str)
//    val out = Enumerator(str).andThen(Enumerator.eof)
//    (in, out)
  }

}

/**
 * 创建ws
 */
class TaskLogWs extends Actor with ActorLogging {
  import context._
  val (logEnumerator, logChannel) = Concurrent.broadcast[JsValue]

  def receive = {
    case join: Join => {
      sender ! Connected(logEnumerator)
      createActor(join.taskId) ! ReadFile(join.taskId)
    }

    case Quit(taskId) => {
      log.info(s"任务$taskId 有一位用户已退出日志查看!")
      createActor(taskId) ! ReadQuit(taskId)
    }

    case na: NotifyAll => {
      log.info(s"notifyAll in TaskLogWs => ${na.kind}, ${na.taskId}, ${na.text}")
      notifyAll(na.kind, na.taskId, na.text)
    }

    case rh: ReadHeader => {
      createActor(rh.taskId) ! rh
    }

    case _ =>
  }

  def createActor(taskId: Int): ActorRef = {
    context.child(s"taskLog_${taskId}").getOrElse(
      actorOf(Props[TaskLog], s"taskLog_${taskId}")
    )
  }

  def notifyAll(kind: String, taskId: Int, text: String) {
    val msg = JsObject(
      Seq(
        "kind" -> JsString(kind)
        ,"taskId" -> JsString(taskId.toString)
        ,"message" -> JsString(text)
      )
    )
    logChannel.push(msg)
  }
}

/**
 * 读取日志文件的actor
 */
class TaskLog extends Actor with ActorLogging{
  val (logEnumerator, logChannel) = Concurrent.broadcast[JsValue]
  //actor引用计数
  var _count = 0
  var _path = ""
  var _from = 0L
  var _logHeaderText = ""
  val _reader = new Reader()
  val _offset = 4*1024L
  var _logStr = ""
  var _logHeadStr = ""
  var _taskId = 0

  def receive = {
    case readFile: ReadFile => {
      _count += 1
      _taskId = readFile.taskId
      _path = getLogFilePath(readFile.taskId)
      val file = new File(_path)
      _from = file.length() - 1024L
      if(_from < 0L){
        _from = 0L
      }
      if(_from > 0){
        _logHeaderText = s"${_from} bytes is hidden, show them ?"
      }
      self ! readFile.taskId
    }

    case rh: ReadHeader => {
      val file = new File(_path)
      if(_logHeadStr.length == 0){
        _logHeadStr = _reader.reader(file, 0L, rh.byteSize.toLong)._1
      }
      context.parent ! NotifyAll("logHeader", rh.taskId, _logHeadStr)
    }

    case readQuit: ReadQuit => {
      _count -= 1
      if(_count == 0){
        context.stop(self)
        log.info(s"TaskLog_${_taskId} Actor已关闭!")
      }
    }

//    case logFirst: LogFirst => {
//      notifyAll("logFirst", _from.toString, logFirst.text)
//    }

    case taskId: Int => {
      if(_logStr == "") {
        _logStr = logReader
      }
      context.parent ! NotifyAll("log", taskId, _logStr)
      context.parent ! NotifyAll("logFirst", taskId, _logHeaderText)
    }
  }

  def getLogFilePath(taskId: Int): String = {
    val filePath = s"${TaskLog.baseLogPath}/${taskId}/result.log"
    new File(filePath).getAbsolutePath
  }



  def logReader: String = {
    if(_path != ""){
      val file = new File(_path)
      val (bufferStr, len) = _reader.reader(file, _from, _offset)
      if(len != -1){
        _from = _from + len.toLong
      }
      bufferStr.toString
    }else{
      "path为空"
    }
  }
}

case class Join(taskId: Int)
case class Connected(enumerator: Enumerator[JsValue])
case class LogFirst(text: String)
case class Quit(taskId: Int)
case class NotifyAll(kind: String, taskId: Int, text: String)


case class ReadFile(taskId: Int)
case class ReadQuit(taskId: Int)
case class ReadHeader(taskId: Int, byteSize: Int)