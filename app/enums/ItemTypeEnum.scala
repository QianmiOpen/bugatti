package enums

import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple._

/**
 * 模板值类型
 * 
 * @author of546
 */
object ItemTypeEnum extends Enumeration {
  type ItemType = Value
  val attribute = Value("attr")
  val variable = Value("var")

  implicit val enumMapper = MappedColumnType.base[ItemType, String](_.toString, this.withName(_))

  implicit val enumReads: Reads[ItemType] = EnumUtils.enumReads(ItemTypeEnum)

  implicit def enumWrites: Writes[ItemType] = EnumUtils.enumWrites

}
