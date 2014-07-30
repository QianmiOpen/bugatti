package actor.task

import com.qianmi.bugatti.actors._

import akka.actor._

/**
  * Created by jinwei on 17/7/14.
 */

import scala.concurrent.duration._

class LookupActor(path: String) extends Actor with ActorLogging {

  val _receiveTimeOut = 120

  sendIdentifyRequest()

  def sendIdentifyRequest(): Unit = {
    context.actorSelection(path) ! Identify(path)
    import context.dispatcher
//    context.setReceiveTimeout(_receiveTimeOut.seconds)
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = {
    case ActorIdentity(`path`, Some(actor)) =>
      log.debug(s"ActorIdentity: ${actor}")
      context.watch(actor)
      context.become(active(actor))
    case ActorIdentity(`path`, None) => {
      log.debug(s"Remote actor not available: $path")
      context.parent ! IdentityNone()
    }
    case ReceiveTimeout              => sendIdentifyRequest()
    case _                           => log.debug("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = {
    case sc: SpiritCommand => {
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

case class LookupActorCommand(commandSeq: Seq[String], taskId: Int, envId: Int, projectId: Int, versionId: Option[Int], order: Int)
case class IdentityNone()