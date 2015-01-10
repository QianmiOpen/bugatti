package actor.task

import akka.actor.SupervisorStrategy.Escalate
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors._

import akka.actor._
import scala.language.postfixOps
/**
  * Created by jinwei on 17/7/14.
 */

import scala.concurrent.duration._

class LookupActor(startPath: String) extends Actor with ActorLogging {
  import context._
  var path = startPath
//  override val supervisorStrategy = OneForOneStrategy() {
//    case e: Exception =>
//      log.error(s"${self} catch ${sender} exception: ${e.getStackTrace}")
//      Escalate
//  }

  sendIdentifyRequest()

  def sendIdentifyRequest(){
    val actorS = context.actorSelection(path)
    log.info(s"actorSelection ==> ${actorS}")
    actorS ! Identify(path)
//    import context.dispatcher
//    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
  }

  def receive = identifying

  def identifying: Actor.Receive = LoggingReceive{
    case ActorIdentity(idPath, Some(actor)) =>
      log.info(s"idpath is ${idPath}")
      log.info(s"path is ${path}")
      log.info(s"ActorIdentity: ${actor}")
      if(idPath == path){
        log.info(s"1")
        context.watch(actor)
        context.become(active(actor))
      }else {
        log.info(s"-1")
        context.stop(actor)
        context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)
      }

    case ActorIdentity(idPath, None) => {
      log.info(s"Remote actor not available: $path")
      context.parent ! IdentityNone()
    }
    case ReceiveTimeout              => {
      log.info(s"ReceiveTimeout...")
      sendIdentifyRequest()
    }
    case _ => log.debug("Not ready yet")
  }

  def active(actor: ActorRef): Actor.Receive = LoggingReceive{
//    case ss: SaltStatus => {
//      log.info(s"remoteActor ${actor} is received in SaltStatus")
//      actor ! ss
//    }
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