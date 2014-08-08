package utils

import play.api.Logger
import play.api.libs.json.JsValue

/**
 * Created by mind on 8/7/14.
 */
object SaltTools {
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
