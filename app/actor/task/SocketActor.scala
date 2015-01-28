package actor.task

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{OneForOneStrategy, Actor, ActorLogging}
import akka.event.LoggingReceive
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.{JsValue, Json}

/**
 * Created by jinwei on 13/7/14.
 */
class SocketActor extends Actor with ActorLogging {

//  override val supervisorStrategy = OneForOneStrategy() {
//    case e: Exception =>
//      log.error(s"${self} catch exception: ${e.getMessage} ${e.getStackTraceString}")
//      Escalate
//  }

  val (out, channel) = Concurrent.broadcast[JsValue]

  def receive = try {
    LoggingReceive {
      case FindLastStatus(key) => {
        notifyAllSocket(Json.obj(s"${key}_last" -> key))
      }
      case jp: JoinProcess => {
        sender ! ConnectedSocket(out)
      }
      case QuitProcess() => {
        log.info("有一个客户端关闭了连接")
      }
      case AllTaskStatus(js) => {
        notifyAllSocket(js)
      }
      case x =>
        log.error(s"socketActor catched unhandled message ${x}")
    }
  }catch {
    case e: Exception => LoggingReceive{
      case _ => log.error(s"socketActor catch Exception:${e.getMessage} ${e.getStackTraceString}")
    }
  }

  def notifyAllSocket(js: JsValue) {
    channel.push(js)
  }
}

case class JoinProcess()
case class ConnectedSocket(out: Enumerator[JsValue])
case class CannotConnect(msg: String)
case class AllTaskStatus(js: JsValue)
case class QuitProcess()