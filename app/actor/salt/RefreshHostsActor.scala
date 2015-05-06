package actor.salt

import actor.ActorUtils
import actor.salt.RefreshHostsActor._
import akka.actor._
import com.qianmi.bugatti.actors.{ListHostsResult, _}
import enums.{ContainerTypeEnum, StateEnum}
import models.conf._
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 * Created by mind on 8/4/14.
 */

object RefreshHostsActor {

  sealed trait State

  object S_Init extends State

  case object S_GetHostList extends State
  case object S_GetHostsInfo extends State
  case object S_Finish extends State
  case class Data(count: Int, hostsState: Map[String, StateEnum.State])

  case object Run
}

class RefreshHostsActor(spiritId: Int, realSender: ActorRef) extends LoggingFSM[State, Data] {
  startWith(S_Init, Data(0, Map.empty))

  when(S_Init) {
    case Event(RefreshHostsActor.Run, _) =>
      goto(S_GetHostList)
  }

  when(S_GetHostList, 10 second) {
    case Event(ListHostsResult(hostNames), date: Data) => {
      if (hostNames == null || hostNames.isEmpty) {
        goto(S_Finish)
      } else {
        val hosts = HostHelper.findBySpiritId(spiritId)
        val noKeyHost = hosts.filterNot(x => hostNames.contains(x.name))
        val oldHosts = hosts.filter(x => hostNames.contains(x.name))
        val oldHostNames = oldHosts.map(_.name)
        val newHosts = hostNames.filterNot(x => oldHostNames.contains(x))

        noKeyHost.foreach { host =>
          HostHelper.updateStateBySpiritId_Name(spiritId, host.name, StateEnum.noKey)
        }

        var count = 0
        var hostsState = Map.empty[String, StateEnum.State]
        newHosts.foreach { name =>
          count += 1
          hostsState += name -> StateEnum.noKey
          ActorUtils.spirits ! RemoteSpirit(spiritId, SaltStatus(name, null, true))
        }

        oldHosts.foreach { host =>
          count += 1
          hostsState += host.name -> host.state
          ActorUtils.spirits ! RemoteSpirit(spiritId, SaltStatus(host.name, host.ip, false))
        }

        goto(S_GetHostsInfo) using date.copy(count = count, hostsState = hostsState)
      }
    }
    case Event(StateTimeout, _) => {
      goto(S_Finish)
    }
  }

  when(S_GetHostsInfo, 30 second) {
    case Event(ssr: SaltStatusResult, t @ Data(count, hostsState)) => {
      val state = if (ssr.canPing && ssr.canSPing) {
        StateEnum.online
      } else if (ssr.canPing && !ssr.canSPing) {
        StateEnum.arrived
      } else {
        StateEnum.offline
      }

      if (ssr.needMInfo) {
        if (ssr.mmInfo.nonEmpty) {
          val ip = getIpFromRet(ssr)
          if (ip != null) {
            HostHelper.create(Host(None, None, None, None, None, "", spiritId, ssr.hostName, ip, 0, state, ContainerTypeEnum.vm, None, None, Seq.empty))
          }
        } else {
          log.warning("Try to get, but no mminfo.")
        }
      } else {
        hostsState.get(ssr.hostName) match {
          case Some(oldState) =>
            if (oldState != state) HostHelper.updateStateBySpiritId_Name(spiritId, ssr.hostName, state)
          case None => // ignore
        }
      }

      if (count == 1) {
        goto(S_Finish) using t.copy(count = 0)
      } else {
        stay using t.copy(count = count - 1)
      }
    }
    case Event(StateTimeout, _) =>
      goto(S_Finish)
  }

  when(S_Finish, 10 second) {
    case Event(StateTimeout, _) => {
      context.stop(self)
      stay
    }
  }

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case S_Init -> S_GetHostList => {
      ActorUtils.spirits ! RemoteSpirit(spiritId, ListHosts())
    }
    case S_GetHostsInfo -> S_Finish => {
      refreshSetHostEnv(spiritId)
    }
  }

  def getIpFromRet(ssr: SaltStatusResult): String = {
    val retJson = Json.parse(ssr.mmInfo)

    retJson.validate[JsObject] match {
      case s: JsSuccess[JsObject] => {
        val ip = (s.get \ "result" \ "return" \ "ip_interfaces" \ "eth0").validate[Seq[String]].get
        log.info(s"$ip")
        if (ip.nonEmpty) {
          ip.head
        } else {
          null
        }
      }
      case e: JsError => {
        log.warning(s"Errors: ${JsError.toFlatJson(e)}")
        null
      }
    }
  }

  def refreshSetHostEnv(spiritId: Int) = {
    log.info("refreshSetHostEnv")
//    val subnetUtils = EnvironmentHelper.all().map { env =>
//      (env.id, env.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String]).map { x => new SubnetUtils(x)})
//    }
//
//    HostHelper.findEmptyEnvsBySyndicName(syndicName).map { envProjectRel =>
//      val env = subnetUtils.filter { case (id, subs) =>
//        subs.exists(sub => sub.getInfo.isInRange(envProjectRel.ip))
//      }
//
//      val envRel = envProjectRel.copy(envId = if (env.size == 0) None else env(0)._1)
//      HostHelper.update(envRel)
//    }
  }

  initialize()
}
