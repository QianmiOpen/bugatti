package actor.task

import akka.actor.SupervisorStrategy.Escalate
import akka.actor.{OneForOneStrategy, Actor, ActorLogging}
import akka.event.LoggingReceive
import play.api.libs.iteratee.{Concurrent, Enumerator}
import play.api.libs.json.{JsObject, JsValue, Json}

/**
 * Created by jinwei on 13/7/14.
 */
class SocketActor extends Actor with ActorLogging {

//  override val supervisorStrategy = OneForOneStrategy() {
//    case e: Exception =>
//      log.error(s"${self} catch exception: ${e.getMessage} ${e.getStackTraceString}")
//      Escalate
//  }
  var channelMap = Map.empty[String, (Enumerator[JsValue], Concurrent.Channel[JsValue])]
  var json: JsValue = Json.obj()

  def receive = try {
    LoggingReceive {
      case FindLastStatus(key) => {
        channelMap.keySet.foreach {
          c =>
            if(key.startsWith(c)){
              channelMap.get(c).get._2.push(Json.obj(s"${key}_last" -> key))
            }
        }
//        notifyAllSocket(Json.obj(s"${key}_last" -> key))
      }
      case jp: JoinProcess => {
        val key = s"${jp.envId}_${jp.projectId}"
        channelMap.get(key) match {
          case Some((out, channel)) =>
            sender ! ConnectedSocket(out)
          case _ =>
            val (out, channel) = Concurrent.broadcast[JsValue]
            channelMap += key -> (out, channel)
            sender ! ConnectedSocket(out)
        }
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
    if(json.equals(js)){
      //json没有变化不推送
    }else {
      json = js
      js.asOpt[JsObject] match {
        case Some(obj) =>
          channelMap.keySet.foreach {
            key =>
              val keySeq = obj.keys.toSeq
              keySeq.foreach(
                s =>
                  if(s.startsWith(key)){
                    channelMap.get(key).get._2.push(Json.obj(s -> js \ s))
                  }
              )
          }
        case _ =>
        //TODO 没有websocket的资源释放

      }
    }
  }
}

case class JoinProcess(envId: Int, projectId: Int)
case class ConnectedSocket(out: Enumerator[JsValue])
case class CannotConnect(msg: String)
case class AllTaskStatus(js: JsValue)
case class QuitProcess()