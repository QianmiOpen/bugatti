package actor.salt

import actor.ActorUtils
import akka.actor.{Cancellable, Actor, ActorLogging, Props}
import akka.event.LoggingReceive
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._
import akka.pattern.ask
import scala.language.postfixOps
import actor.salt.RefreshSpiritsActor._

/**
 * Created by mind on 7/31/14.
 */


object RefreshSpiritsActor {
  case class RefreshHosts(spiritId: Int) // refresh host include refresh files
  case class RefreshFiles(spiritId: Int) // refresh file include refresh file server and returner

  case object RefreshSpiritsHosts
}

class RefreshSpiritsActor extends Actor with ActorLogging {
  import context._
  implicit val timeout = Timeout(60 seconds)

  var refreshSchedule: Cancellable = _

  override def preStart(): Unit = {
//    refreshSchedule = context.system.scheduler.schedule(600 seconds, 3600 * 24 seconds, self, RefreshSpiritsHosts)
  }

  override def postStop(): Unit = {
    if (refreshSchedule != null) {
      refreshSchedule.cancel()
    }
  }

  override def receive = LoggingReceive {
    case RefreshSpiritsHosts => {
      val future = ActorUtils.spirits ? ConnectedSpirits
      val spiritIds = Await.result(future, timeout.duration).asInstanceOf[Seq[Int]]

      log.debug(s"Auto refresh host info, spiritIds: ${spiritIds}")
      spiritIds.foreach { spiritId =>
        self ! RefreshHosts(spiritId)
      }
    }

    case RefreshHosts(spiritId) => {
      log.debug(s"Refresh Hosts, begin refresh hosts: ${spiritId}")
      val rha = context.actorOf(Props(classOf[RefreshHostsActor], spiritId, sender))
      rha ! RefreshHostsActor.Run
    }

    case RefreshFiles(spiritId) => {
      val rfa = context.actorOf(Props(classOf[RefreshFilesActor], spiritId, sender))
      rfa ! RefreshFilesActor.Run
    }

    case x => log.debug(s"RefreshSpiritsActor receive unknown message ${x}")
  }
}
