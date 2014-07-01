package utils

import play.api.Logger

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {

  def isSnapshot(version: String): Boolean = {
    val result = version.contains("-SNAPSHOT")
    Logger.info("isSnapshot ==>" + result.toString)
    result
  }
}
