package actor.salt

import actor.salt.RefreshHostsActor._
import akka.actor._
import com.qianmi.bugatti.actors.{SaltJobOk, SaltStatusResult}
import enums.ContainerTypeEnum
import models.conf._
import org.apache.commons.net.util.SubnetUtils
import play.api.libs.json.{JsError, JsObject, JsSuccess, Json}

import scala.language.postfixOps
import scala.concurrent.duration._

/**
 * Created by mind on 8/4/14.
 */

case class Run()

object RefreshHostsActor {
  sealed trait State
  case object S_Init extends State
  case object S_GetHostsInfo extends State
  case object S_Finish extends State
}

class RefreshHostsActor(spiritId: Int, realSender: ActorRef) extends LoggingFSM[State, Seq[SaltStatusResult]] {
  val commands = Seq(
    Seq("salt-run", "fileserver.update"),
    Seq("salt", "*", "saltutil.sync_returners"),
    Seq("salt", "*", "grains.get", "ip_interfaces:eth0")
  )

  startWith(S_Init, Seq.empty)

  when(S_Init) {
    case Event(Run, _) =>
      goto(S_GetHostsInfo)
  }

  when(S_GetHostsInfo, 60 second) {
    case Event(StateTimeout, _) => {
      goto(S_Finish)
    }
  }

  when(S_Finish)(FSM.NullFunction)

  whenUnhandled {
    case Event(e, s) =>
      log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
      stay
  }

  onTransition {
    case S_Init -> S_GetHostsInfo =>

  }

  def parseResult(sr: SaltJobOk) = {
    val retJson = Json.parse(sr.result)

    retJson.validate[Seq[JsObject]] match {
      case s: JsSuccess[Seq[JsObject]] => {
        AreaHelper.findById(spiritId) match {
          case Some(area) => {
            val hostList = s.get.map { hostJson =>
              ((hostJson \ "result" \ "id").validate[String].get,
                (hostJson \ "result" \ "return").validate[Seq[String]].asOpt.getOrElse(Seq("")).head
                )
            }

            val addedHosts = HostHelper.findBySyndicName(area.syndicName).map(_.name)
            val newHosts = hostList.filterNot(x => x._2.isEmpty).filterNot(x => addedHosts.contains(x._1))

            newHosts.foreach { newhost =>
              HostHelper.create(Host(None, None, None, None, area.syndicName, newhost._1, newhost._2, ContainerTypeEnum.vm, None, None, Seq.empty[Variable]))
            }

            refreshSetHostEnv(area.syndicName)
          }
          case None => log.debug(s"Unknown area: ${spiritId}") // ignore
        }
      }
      case e: JsError => log.warning(s"Errors: ${JsError.toFlatJson(e)}")
    }
  }

  def refreshSetHostEnv(syndicName: String) = {
    val subnetUtils = EnvironmentHelper.all().map { env =>
      (env.id, env.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String]).map { x => new SubnetUtils(x)})
    }

    HostHelper.findEmptyEnvsBySyndicName(syndicName).map { envProjectRel =>
      val env = subnetUtils.filter { case (id, subs) =>
        subs.exists(sub => sub.getInfo.isInRange(envProjectRel.ip))
      }

      val envRel = envProjectRel.copy(envId = if (env.size == 0) None else env(0)._1)
      HostHelper.update(envRel)
    }
  }
}
