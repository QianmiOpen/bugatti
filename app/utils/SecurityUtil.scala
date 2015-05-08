package utils

import play.api.Logger
import play.api.libs.Crypto

/**
 * 加密工具
 */
object SecurityUtil {

  implicit val app: play.api.Application = play.api.Play.current

  val u_ssh_key = app.configuration.getString("user.ssh_key").getOrElse("i4Epmvt3p?dG2si8")

  /**
   * 加密KEY
   * @param value 无密内容
   * @return
   */
  def encryptUK(value: String): String = {
    try {
      Crypto.encryptAES(value, u_ssh_key)
    } catch {
      case e: Exception =>
        Logger.error("encrypt uk error")
        value
    }
  }

  /**
   * 解密KEY
   * @param value 加密内容
   * @return
   */
  def decryptUK(value: String): String = {
    try {
      Crypto.decryptAES(value, u_ssh_key)
    } catch {
      case e: Exception =>
        Logger.error("decrypt uk error")
        value
    }
  }

}
