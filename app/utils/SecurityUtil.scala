package utils

import play.api.libs.Crypto

/**
 * 加密工具
 */
object SecurityUtil {

  implicit val app: play.api.Application = play.api.Play.current

  val u_ssh_key = app.configuration.getString("user.ssh_key").getOrElse("i4Epmvt3p?dG2si8")

  /**
   * 加密用户KEY
   * @param value 无密内容
   * @return
   */
  def encryptUK(value: String): String = Crypto.encryptAES(value, u_ssh_key)

  /**
   * 解密用户KEY
   * @param value 加密内容
   * @return
   */
  def decryptUK(value: String): String = Crypto.decryptAES(value, u_ssh_key)

}
