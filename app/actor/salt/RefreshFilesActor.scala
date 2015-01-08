package actor.salt

import actor.ActorUtils
import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors.{SaltTimeOut, SaltJobOk, SaltCommand}

/**
 * Created by mind on 8/4/14.
 */
class RefreshFilesActor(areaId: Int, realSender: ActorRef) extends Actor with ActorLogging {
  val commands = Seq(
    Seq("salt-run", "fileserver.update"),
    Seq("salt", "*", "saltutil.sync_returners")
  )

  var step = 0

  override def receive = LoggingReceive {
    case Run => {
      if (step < commands.length) {
        log.debug(s"Refresh files run ${commands(step)}")
        ActorUtils.areas ! RemoteSpirit(SaltCommand(commands(step)), areaId = areaId)
        step += 1
      } else {
        realSender ! Finish

        context.stop(self)
      }
    }

    case sr: SaltJobOk => {
      self ! Run
    }

    case SaltTimeOut() => {
      realSender ! Error

      context.stop(self)
    }

    case ConnectStoped => {
      sender ! Error

      context.stop(self)
    }

    case x => log.debug(s"RefreshFilesActor receive unknown message ${x}")
  }
}
