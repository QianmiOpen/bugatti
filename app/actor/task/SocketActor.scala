package actor.task

import akka.actor.Actor
import play.api.Logger
import play.api.libs.iteratee.{Enumerator, Concurrent}
import play.api.libs.json.JsValue

/**
 * Created by jinwei on 13/7/14.
 */
class SocketActor extends Actor{
  val (out, channel) = Concurrent.broadcast[JsValue]

  def receive = {
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
    Logger.info("notifyAllSocket==>" + js.toString())
    channel.push(js)
  }
}

case class JoinProcess(js: JsValue)
case class ConnectedSocket(out: Enumerator[JsValue])
case class CannotConnect(msg: String)
case class AllTaskStatus()
case class QuitProcess()