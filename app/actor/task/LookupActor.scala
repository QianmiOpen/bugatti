package actor.task

import akka.actor.SupervisorStrategy.Escalate
import com.qianmi.bugatti.actors._

import akka.actor._

/**
  * Created by jinwei on 17/7/14.
 */

import scala.concurrent.duration._

class LookupActor(path: String) extends Actor with ActorLogging {

  override val supervisorStrategy = OneForOneStrategy() {
    case e: Exception =>
      log.error(s"${self} catch ${sender} exception: ${e.getStackTrace}")
      Escalate
  }

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    val actorS = context.actorSelection(path)
    log.info(s"actorSelection ==> ${actorS}")
    actorS ! Identify(path)
    import context.dispatcher
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(idpath, Some(actor)) =>
      log.info(s"idpath is ${idpath}")
      log.info(s"path is ${path}")
      log.info(s"ActorIdentity: ${actor}")
      context.watch(actor)
      context.become(active(actor))
    case ActorIdentity(`path`, None) => {
      log.info(s"Remote actor not available: $path")
      context.parent ! IdentityNone()
    }
    case ReceiveTimeout              => {
      log.info(s"ReceiveTimeout...")
      sendIdentifyRequest()
    }
    case _                           => log.debug("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case ss: SaltStatus => {
      log.info(s"remoteActor ${actor} is received in SaltStatus")
      actor ! ss
    }
    case sc: SpiritCommand => {
      log.info(s"remoteActor ${actor} is received !")
      actor ! sc
    }
    case sr: SpiritResult => {
      context.parent ! sr
    }
    case Terminated(`actor`) =>
      context.parent ! IdentityNone()
      log.debug("Actor terminated")
//      sendIdentifyRequest()
//      context.become(identifying)
    case ReceiveTimeout =>{
//      ignore
    }
  }
}

case class IdentityNone()