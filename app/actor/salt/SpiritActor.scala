package actor.salt

import akka.actor._
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors.SpiritCommand

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by mind on 8/1/14.
 */
case class Reconnect(path: String)
case object ConnectStoped
case class Connected()

class SpiritActor(startPath: String) extends Actor with ActorLogging {
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
        context.watch(actor)
        context.become(active(actor))
      } else {
        context.stop(actor)
        context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
      }
    }
    case ActorIdentity(idPath, None) => {
      context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
    }
    case Reconnect(newPath)          => path = newPath
    case ReceiveTimeout              => sendIdentifyRequest()
    case Connected                   => sender ! false
    case _                           => {
      sender ! ConnectStoped
      log.warning("Not ready yet")
    }
  }

  def active(actor: ActorRef): Actor.Receive = LoggingReceive {
    case Reconnect(newPath) => {
      context.children.foreach { child =>
        child ! ConnectStoped
      }

      path = newPath
      sendIdentifyRequest()
      context.become(identifying)
    }

    case sc: SpiritCommand => {
      actor forward  sc
    }

    case Connected => sender ! true

    case Terminated(`actor`) => {
      context.children.foreach { child =>
        child ! ConnectStoped
      }

      sendIdentifyRequest()
      context.become(identifying)
    }
    case ReceiveTimeout => log.warning("ReceiveTimeout")//      ignore

    case x => log.warning(s"Unknown message $x")
  }
}
