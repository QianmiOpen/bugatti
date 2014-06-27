package enums

import play.api.libs.json.{Writes, Reads}

import scala.slick.driver.MySQLDriver.simple._
/**
 * 成员\环境级别
 *
 * @author of546
 */
object LevelEnum extends Enumeration {
  type Level = Value
  val unsafe = Value(0, "unsafe") // 非安全级别
  val safe = Value(1, "safe")     // 安全级别

  implicit val enumMapper = MappedColumnType.base[Level, Int](_.id, this.apply)

  implicit val enumReads: Reads[Level] = EnumUtils.enumReads(LevelEnum)

  implicit def enumWrites: Writes[Level] = EnumUtils.enumWrites
}
