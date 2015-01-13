package enums

import play.api.libs.json.{Writes, Reads}
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by mind on 1/12/15.
 */
object StateEnum extends Enumeration {
  type State = Value

  val noKey = Value(1, "no salt key")
  val offline = Value(2, "can't ping and test ping")
  val arrived = Value(3, "only can ping")
  val online = Value(4, "can ping and test ping")

  implicit val enumMapper = MappedColumnType.base[State, Int](_.id, this.apply)

  implicit val enumReads: Reads[State] = EnumUtils.enumReads(StateEnum)

  implicit def enumWrites: Writes[State] = EnumUtils.enumWrites

}
