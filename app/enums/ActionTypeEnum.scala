package enums

import play.api.libs.json.{Writes, Reads}
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by mind on 8/19/14.
 */
object ActionTypeEnum extends Enumeration {
  type ActionType = Value
  val project = Value("project")
  val host = Value("host")

  implicit val enumMapper = MappedColumnType.base[ActionType, String](_.toString, this.withName(_))

  implicit val enumReads: Reads[ActionType] = EnumUtils.enumReads(ActionTypeEnum)

  implicit def enumWrites: Writes[ActionType] = EnumUtils.enumWrites

}
