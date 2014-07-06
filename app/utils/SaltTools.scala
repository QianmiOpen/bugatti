package utils

import org.apache.commons.net.util.SubnetUtils
import play.api.{Application, Logger}

import sys.process._
import models.conf._

/**
 * Created by mind on 7/6/14.
 */
object SaltTools {
  def refreshHostList(app: Application) = {
    if (app.configuration.getBoolean("salt.init").getOrElse(false)) {
      refreshHostList
    }
  }

  def refreshHostList = {
    val subnetUtils = getSubnetUtils
    AreaHelper.all.foreach { area =>
      val hosts = Seq("salt", area.syndicName, "cmd.run", "salt-run manage.up", "--out=txt").lines.map{
        _.replaceAll(s"^${area.syndicName}: +", "")
      }
      Logger.debug(s"Get hosts: ${hosts}")

      val addedHosts = EnvironmentProjectRelHelper.findBySyndicName(area.syndicName).map {
        _.name
      }
      val newHosts = hosts.filterNot(addedHosts.contains)

      Seq("salt", "-L", newHosts.mkString(","), "grains.get", "ip_interfaces:eth0", "--out=txt").lines.foreach { x =>
        val groupMatch = """^([^:]+): +\['([^']+)'\]""".r.findAllIn(x).matchData
        if (groupMatch.hasNext){
          val groups = groupMatch.next
          val ip = groups.group(2)
          EnvironmentProjectRelHelper.create(EnvironmentProjectRel(None, getEnvIdFromIp(subnetUtils, ip), None, area.syndicName, groups.group(1), ip))
        }else{
          Logger.info(s"Parse host fail: ${x}")
        }
      }
    }
  }

  def getSubnetUtils: Seq[(Option[Int], Seq[SubnetUtils])] = {
    EnvironmentHelper.all().map { env =>
      (env.id, env.ipRange.map(_.split(",").toList).getOrElse(Seq.empty[String]).map { x => new SubnetUtils(x)})
    }
  }

  def getEnvIdFromIp(subnetUtils: Seq[(Option[Int], Seq[SubnetUtils])], ip: String): Option[Int] = {
    val env = subnetUtils.filter { case (id, subs) =>
      subs.exists(sub => sub.getInfo.isInRange(ip))
    }

    if (env.size == 0) {
      None
    } else {
      env(0)._1
    }
  }
}
