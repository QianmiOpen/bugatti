package actor.salt

import actor.ActorUtils
import akka.actor.{ActorRef, Actor, ActorLogging}
import akka.event.LoggingReceive
import com.qianmi.bugatti.actors.{SaltTimeOut, SaltJobOk, SaltCommand}
import enums.ContainerTypeEnum
import models.conf._
import org.apache.commons.net.util.SubnetUtils
import play.api.libs.json.{JsObject, JsError, JsSuccess, Json}

/**
 * Created by mind on 8/4/14.
 */
class RefreshHostsActor(areaId: Int, realSender: ActorRef) extends Actor with ActorLogging {
  val commands = Seq(
    Seq("salt-run", "fileserver.update"),
    Seq("salt", "*", "saltutil.sync_returners"),
    Seq("salt", "*", "grains.get", "ip_interfaces:eth0")
  )

  var step = 0

  var saltResult: SaltJobOk = _

  override def receive = LoggingReceive {
    case Run => {
      if (step < commands.length) {
        log.debug(s"Refresh files run ${commands(step)}")
        ActorUtils.areas ! RemoteSpirit(SaltCommand(commands(step)), areaId = areaId)
        step += 1
      } else {
        parseResult(saltResult)

        realSender ! Finish

        context.stop(self)
      }
    }

    case sr: SaltJobOk => {
      saltResult = sr
      self ! Run
    }

    case SaltTimeOut() => {
      realSender ! Error

      context.stop(self)
    }

    case x => log.debug(s"Unknown message ${x}")
  }

  def parseResult(sr: SaltJobOk) = {
    val retJson = Json.parse(sr.result)

    retJson.validate[Seq[JsObject]] match {
      case s: JsSuccess[Seq[JsObject]] => {
        AreaHelper.findById(areaId) match {
          case Some(area) => {
            val hostList = s.get.map { hostJson =>
              ((hostJson \ "result" \ "id").validate[String].get,
                (hostJson \ "result" \ "return").validate[Seq[String]].asOpt.getOrElse(Seq("")).head
                )
            }

            val addedHosts = EnvironmentProjectRelHelper.findBySyndicName(area.syndicName).map(_.name)
            val newHosts = hostList.filterNot(x => x._2.isEmpty).filterNot(x => addedHosts.contains(x._1))

            newHosts.foreach { newhost =>
              EnvironmentProjectRelHelper.create(EnvironmentProjectRel(None, None, None, None, area.syndicName, newhost._1, newhost._2, ContainerTypeEnum.vm, None, None, Seq.empty[Variable]))
            }

            refreshSetHostEnv(area.syndicName)
          }
          case None => log.debug(s"Unknown area: ${areaId}") // ignore
        }
      }
      case e: JsError => log.warning(s"Errors: ${JsError.toFlatJson(e)}")
    }
  }

  def refreshSetHostEnv(syndicName: String) = {
    val subnetUtils = EnvironmentHelper.all().map { env =>
      (env.id, env.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String]).map { x => new SubnetUtils(x)})
    }

    EnvironmentProjectRelHelper.findEmptyEnvsBySyndicName(syndicName).map { envProjectRel =>
      val env = subnetUtils.filter { case (id, subs) =>
        subs.exists(sub => sub.getInfo.isInRange(envProjectRel.ip))
      }

      val envRel = envProjectRel.copy(envId = if (env.size == 0) None else env(0)._1)
      EnvironmentProjectRelHelper.update(envRel)
    }
  }
}
