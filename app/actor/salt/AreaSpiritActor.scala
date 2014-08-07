package actor.salt

import akka.actor._
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors.SpiritCommand

import scala.concurrent.duration._

/**
 * Created by mind on 8/1/14.
 */
case class Reconnect(path: String)
case class ConnectStoped()
case class Connected()

class AreaSpiritActor(startPath: String) extends Actor with ActorLogging {
  import context._
  var path = startPath

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
  }

  override def postStop = {
    context.children.foreach { child =>
      child ! ConnectStoped
    }
  }

  def receive = identifying

  def identifying: Actor.Receive = LoggingReceive {
    case ActorIdentity(idPath, Some(actor)) => {
      if (idPath == path) {
        log.debug(s"ActorIdentity: ${actor}")
        context.watch(actor)
        context.become(active(actor))
      } else {
        context.stop(actor)
        context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
      }
    }
    case ActorIdentity(idPath, None) => {
      log.debug(s"Remote actor not available: ${idPath}")
      context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
    }
    case Reconnect(newPath)          => path = newPath
    case ReceiveTimeout              => sendIdentifyRequest()
    case Connected                   => sender ! false
    case _                           => {
      sender ! ConnectStoped
      log.debug("Not ready yet")
    }
  }

  def active(actor: ActorRef): Actor.Receive = LoggingReceive {
    case Reconnect(newPath) => {
      log.debug(s"Reconnect new path is : ${newPath}")

      context.children.foreach { child =>
        child ! ConnectStoped
      }

      path = newPath
      sendIdentifyRequest()
      context.become(identifying)
    }

    case sc: SpiritCommand => {
      log.debug(s"Run Spirit command: ${self}, ${sc}")
      val spiritCmd = context.actorOf(Props(classOf[SpiritCommandActor], sender))
      actor.!(sc)(spiritCmd)
    }

    case Connected => sender ! true

    case Terminated(`actor`) => {
      log.debug("Actor terminated")

      context.children.foreach { child =>
        child ! ConnectStoped
      }

      sendIdentifyRequest()
      context.become(identifying)
    }
    case ReceiveTimeout => //      ignore
  }
}
