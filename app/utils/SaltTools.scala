package utils

import org.apache.commons.net.util.SubnetUtils
import play.api.libs.json.JsValue
import play.api.{Application, Logger}

import scala.util.parsing.json.JSONObject
import sys.process._
import models.conf._

/**
 * Created by mind on 7/6/14.
 */
object SaltTools {
  def refreshHostList(app: Application) {
    if (app.configuration.getBoolean("salt.init").getOrElse(false)) {
      refreshHostList
    }
  }

  def refreshHostList = {
    AreaHelper.allInfo.foreach { area =>
      refreshHost(area.syndicName)
    }
  }

  def refreshHost(syndicName: String) = {
    refreshSyndic(syndicName)
    refreshSetHostEnv(syndicName)
  }

  def refreshSyndic(syndicName: String) = {
    val hosts = Seq("salt", syndicName, "cmd.run", "salt-run manage.up", "--out=txt").lines.map{
      _.replaceAll(s"^${syndicName}: +", "")
    }
    Logger.debug(s"Get hosts: ${hosts}")

    val addedHosts = EnvironmentProjectRelHelper.findBySyndicName(syndicName).map {
      _.name
    }
    val newHosts = hosts.filterNot(addedHosts.contains)

    Seq("salt", "-L", newHosts.mkString(","), "grains.get", "ip_interfaces:eth0", "--out=txt").lines.foreach { x =>
      val groupMatch = """^([^:]+): +\['([^']+)'\]""".r.findAllIn(x).matchData
      if (groupMatch.hasNext){
        val groups = groupMatch.next
        val ip = groups.group(2)
        EnvironmentProjectRelHelper.create(EnvironmentProjectRel(None, None, None, syndicName, groups.group(1), ip))
      }else{
        Logger.info(s"Parse host fail: ${x}")
      }
    }
  }

  def refreshSetHostEnv(syndicName: String) = {
    val subnetUtils = EnvironmentHelper.all().map { env =>
      (env.id, env.ipRange.map(_.split(";").toList).getOrElse(Seq.empty[String]).map { x => new SubnetUtils(x)})
    }

    EnvironmentProjectRelHelper.findEmptyEnvsBySyndicName(syndicName).map{ envProjectRel =>
      val env = subnetUtils.filter { case (id, subs) =>
        subs.exists(sub => sub.getInfo.isInRange(envProjectRel.ip))
      }

      val envRel = envProjectRel.copy(envId = if (env.size == 0) {None} else {env(0)._1})
      EnvironmentProjectRelHelper.update(envRel)
    }
  }

  def findErrorConf(paramsJson: JsValue, str: String): Seq[String] = {
    var errorConf = Set.empty[String]
    Logger.info(s"errorConf ==> ${errorConf}")
    """\{\{ *[^}]+ *\}\}""".r.findAllIn(str).foreach{
      key => {
        Logger.info(s"${key}")
        val realKey: String = key.replaceAll("\\{\\{", "").replaceAll("\\}\\}", "")
        Logger.info(s"${(paramsJson \ realKey).asOpt[String]}")
        (paramsJson \ realKey).asOpt[JsValue] match {
          case Some(j) =>
            Logger.info(s"some ==> ${key}")
            //ignore
          case _ =>
            Logger.info(s"else ==> ${key}")
            errorConf = errorConf + realKey
        }
      }
    }
    Logger.info(s"errorConf ==> ${errorConf}")
    errorConf.toSeq
  }
}
