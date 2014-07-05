package controllers.actor

import akka.actor._
import controllers.actor.TaskProcess
import play.api.Logger
import play.api.libs.iteratee._
import play.api.libs.json.JsValue


/**
 * 处理所有的websocket
 * Created by jinwei on 1/7/14.
 */
class SocketActor extends Actor {

  val (out, channel) = Concurrent.broadcast[JsValue]

  def receive = {
    case JoinProcess(js) => {
      sender ! ConnectedSocket(out)
      notifyAllSocket(js)
    }
    case QuitProcess() => {
      Logger.info("有一个客户端关闭了连接")
    }
    case AllTaskStatus() => {
      notifyAllSocket(TaskProcess.getAllStatus)
    }
  }

  def notifyAllSocket(js: JsValue) {
    Logger.info("notifyAllSocket==>" + js.toString())
    Thread.sleep(100)
    channel.push(js)
  }
}

case class JoinProcess(js: JsValue)

case class AllTaskStatus()

case class ConnectedSocket(out: Enumerator[JsValue])

case class CannotConnect(msg: String)

case class QuitProcess()
