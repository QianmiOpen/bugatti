package actor.task

import java.io.File

import akka.actor.Actor
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Concurrent}
import play.api.libs.json.{Json, JsValue}
import utils.SaltTools
import scala.sys.process._

/**
 * Created by jinwei on 13/7/14.
 */
class SocketActor extends Actor{
  val (out, channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case FindLastStatus(key) => {
      notifyAllSocket(Json.obj(s"${key}_last" -> key))
    }
    case JoinProcess(js) => {
      sender ! ConnectedSocket(out)
    }
    case QuitProcess() => {
      Logger.info("有一个客户端关闭了连接")
    }
    case AllTaskStatus() => {
      notifyAllSocket(MyActor.statusMap)
    }
    case "notify" => {
      self ! AllTaskStatus()
    }
  }

  def notifyAllSocket(js: JsValue) {
    Logger.debug("notifyAllSocket==>" + js.toString())
    channel.push(js)
    //test
//    val _baseLogPath = SaltTools.logPath
//    val path = s"${_baseLogPath}/saltLogs.log"
//    val baseFile = new File(_baseLogPath)
//    if(!baseFile.exists()){
//      baseFile.mkdirs()
//    }
//    val file = new File(path)
//    if(!file.exists()){
//      file.createNewFile()
//    }
//    (Seq("echo", Json.stringify(js)) #>> file lines)

  }
}

case class JoinProcess(js: JsValue)
case class ConnectedSocket(out: Enumerator[JsValue])
case class CannotConnect(msg: String)
case class AllTaskStatus()
case class QuitProcess()