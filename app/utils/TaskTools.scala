package utils

import java.util.{List => JList, Map => JMap}

import models.conf._
import play.api.{Logger, Play}

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {
  /**
   * 判断版本是否是snapshot
   * @param versionId
   * @return
   */
  def isSnapshot(versionId: Int): Boolean = {
    var result = true
    VersionHelper.findById(versionId) match {
      case Some(version) => {
        result = version.vs.endsWith("-SNAPSHOT")
      }
      case _ => {
        result = false
      }
    }
    Logger.info("isSnapshot ==>" + result.toString)
    result
  }

  /**
   * 去除字符串两边的引号
   * @param s
   * @return
   */
  def trimQuotes(s: String): String = {
    s.trim.stripPrefix("\"").stripSuffix("\"").trim
  }
}

object ConfHelp {
  val app = Play.current

  lazy val logPath = app.configuration.getString("salt.log.dir").getOrElse("target/saltlogs")

  lazy val confPath: String = app.configuration.getString("salt.file.pkgs").getOrElse("target/pkgs")
}

