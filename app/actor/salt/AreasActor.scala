package actor.salt

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.util.Timeout
import com.qianmi.bugatti.actors.SpiritCommand
import models.conf.Area

import scala.collection._

import akka.pattern.ask

import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Created by mind on 8/1/14.
 */

case class AddArea(area: Area)

case class UpdateArea(area: Area)

case class DeleteArea(areaId: Int)

case class RemoteSpirit(saltCommand: SpiritCommand, syndicName: String = "", areaId: Int = -1)

case class ConnectedAreas()

class AreasActor extends Actor with ActorLogging {
  val DefaultAreaSpiritPort = 2552

  val areaMap = mutable.Map.empty[Int, Area]

  val areaIdMap = mutable.Map.empty[Int, ActorRef]

  val areaSyndicIdMap = mutable.Map.empty[String, Int]

  val RemotePath = s"akka.tcp://Spirit@%s:${DefaultAreaSpiritPort}/user/SpiritCommands"

  val AreaName = s"Area_%d"

  override def receive = LoggingReceive {
    case AddArea(area) => {
      val areaId = area.id.get

      areaMap.get(areaId) match {
        case Some(oldArea) => log.error(s"Add exists area: ${oldArea}.")
        case None => {
          val areaSpiritActor = context.actorOf(Props(classOf[AreaSpiritActor], RemotePath.format(area.syndicIp)), name = AreaName.format(areaId))

          areaIdMap.put(areaId, areaSpiritActor)
          areaSyndicIdMap.put(area.syndicName, areaId)
          areaMap.put(areaId, area)

          log.debug(s"Add area, Actor is ${areaSpiritActor}")
        }
      }
    }

    case UpdateArea(area) => {
      val areaId = area.id.get

      areaMap.get(areaId) match {
        case Some(oldArea) => {
          if (oldArea.syndicIp != area.syndicIp) {
            log.debug(s"Area ip changed, from ${oldArea.syndicIp} to ${area.syndicIp}")

            context.child(AreaName.format(areaId)).foreach { areaActor =>
              areaActor ! Reconnect(RemotePath.format(area.syndicIp))
            }
          }

          if (oldArea.syndicName != area.syndicName) {
            areaSyndicIdMap.remove(oldArea.syndicName)
            areaSyndicIdMap.put(area.syndicName, areaId)
          }

          areaMap.put(areaId, area)
        }
        case None => // ignore
      }
    }

    case DeleteArea(areaId) => {
      context.child(AreaName.format(areaId)).foreach { areaActor =>
        context.stop(areaActor)
      }

      areaMap.get(areaId) match {
        case Some(area) => {
          areaIdMap.remove(areaId)
          areaMap.remove(areaId)
          areaSyndicIdMap.remove(area.syndicName)
        }
        case None => // ignore
      }
    }

    case rsc: RemoteSpirit => {
      val areaId = if (rsc.areaId == -1) {
        areaSyndicIdMap.get(rsc.syndicName).getOrElse(-1)
      } else {
        rsc.areaId
      }

      areaIdMap.get(areaId) match {
        case Some(areaActor) => areaActor.!(rsc.saltCommand)(sender)
        case None => sender ! ConnectStoped
      }
    }

    case ConnectedAreas => {
      implicit val timeout = Timeout(5 seconds)

      val areasFuture = areaIdMap.map { case (areaId, areaActor) =>
        (areaId, areaActor ? Connected)
      }

      val areasResult = areasFuture.map { case (areaId, areaFuture) =>
        (areaId, Await.result(areaFuture, timeout.duration).asInstanceOf[Boolean])
      }

      sender ! areasResult.filter(_._2).map(_._1)
    }

    case x => log.debug(s"Unknown ${x}")
  }
}
