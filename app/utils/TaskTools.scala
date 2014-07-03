package utils

import play.api.Logger

/**
 * Created by jinwei on 1/7/14.
 */
object TaskTools {
  /**
   * 判断版本是否是snapshot
   * @param version
   * @return
   */
  def isSnapshot(version: String): Boolean = {
    val result = version.contains("-SNAPSHOT")
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
