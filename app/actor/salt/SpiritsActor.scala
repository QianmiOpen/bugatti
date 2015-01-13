package actor.salt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.pattern.ask
import akka.util.Timeout
import com.qianmi.bugatti.actors.SpiritCommand
import models.conf.Spirit

import scala.collection._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by mind on 8/1/14.
 */

case class AddSpirit(spirit: Spirit)

case class UpdateSpirit(spirit: Spirit)

case class DeleteSpirit(spiritId: Int)

case class RemoteSpirit(spiritId: Int = -1, saltCommand: SpiritCommand)

case class ConnectedSpirits()

class SpiritsActor extends Actor with ActorLogging {
  val DefaultSpiritPort = 2552

  val spiritMap = mutable.Map.empty[Int, Spirit]

  val RemotePath = s"akka.tcp://Spirit@%s:${DefaultSpiritPort}/user/SpiritCommands"

  val SpiritName = s"Spirit_%d"

  override def receive = LoggingReceive {
    case rsc: RemoteSpirit => {
      context.child(SpiritName.format(rsc.spiritId)) match {
        case Some(spiritActor) => spiritActor forward rsc.saltCommand
        case None => sender ! ConnectStoped
      }
    }

    case AddSpirit(spirit) => {
      val spiritId = spirit.id.get

      spiritMap.get(spiritId) match {
        case Some(oldSpirit) => log.error(s"Add exists spirit: ${oldSpirit}.")
        case None => {
          val spiritActor = context.actorOf(Props(classOf[SpiritActor], RemotePath.format(spirit.ip)), name = SpiritName.format(spiritId))

          spiritMap.put(spiritId, spirit)

          log.debug(s"Add spirit, Actor is ${spiritActor}")
        }
      }
    }

    case UpdateSpirit(spirit) => {
      val spiritId = spirit.id.get

      spiritMap.get(spiritId) match {
        case Some(oldSpirit) => {
          if (oldSpirit.ip != spirit.ip) {
            log.info(s"Spirit ip changed, from ${oldSpirit.ip} to ${spirit.ip}")

            context.child(SpiritName.format(spiritId)).foreach { spiritActor =>
              spiritActor ! Reconnect(RemotePath.format(spirit.ip))
            }
          }

          spiritMap.put(spiritId, spirit)
        }
        case None => // ignore
      }
    }

    case DeleteSpirit(spiritId) => {
      context.child(SpiritName.format(spiritId)).foreach { spiritActor =>
        context.stop(spiritActor)
      }

      spiritMap.get(spiritId) match {
        case Some(area) => {
          spiritMap.remove(spiritId)
        }
        case None => // ignore
      }
    }

    case ConnectedSpirits => {
      implicit val timeout = Timeout(5 seconds)

      val spiritsFuture = spiritMap.map { case (spiritId, _) =>
        context.child((SpiritName.format(spiritId))) match {
          case Some(spiritActor) => (spiritId, spiritActor ? Connected)
          case None => (spiritId, Actor.noSender ? Connected)
        }
      }

      val spiritsResult = spiritsFuture.map { case (spiritId, spiritFuture) =>
        (spiritId, Await.result(spiritFuture, timeout.duration).asInstanceOf[Boolean])
      }

      sender ! spiritsResult.filter(_._2).map(_._1)
    }

    case x => log.debug(s"Unknown ${x}")
  }
}
