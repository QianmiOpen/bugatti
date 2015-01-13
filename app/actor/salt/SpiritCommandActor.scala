package actor.salt

import actor.salt.ConnectStoped._
import akka.actor.{ActorRef, Cancellable, Actor, ActorLogging}
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors.{SaltTimeOut, SpiritResult}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by mind on 8/3/14.
 */
class SpiritCommandActor(realSender: ActorRef) extends Actor with ActorLogging {
  import context._

  var timeOutSchedule: Cancellable = _

  val TimeOutSeconds = 900 seconds

  override def preStart(): Unit = {
    timeOutSchedule = context.system.scheduler.scheduleOnce(TimeOutSeconds) {
      realSender ! SaltTimeOut
      context.stop(self)
    }
  }

  override def postStop(): Unit = {
    if (timeOutSchedule != null) {
      timeOutSchedule.cancel()
    }
  }

  override def receive = LoggingReceive {
    case sr: SpiritResult => {
      realSender ! sr
//      context.stop(self)

    }

    case ConnectStoped => {
      realSender ! ConnectStoped
      context.stop(self)
    }

    case x => log.debug(s"Unknown message: ${x}")
  }
}
