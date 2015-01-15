package enums

import play.api.libs.json.{Writes, Reads}
import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by mind on 1/12/15.
 */
object StateEnum extends Enumeration {
  type State = Value

  val noKey = Value(0, "no salt key")
  val offline = Value(1, "can't ping and test ping")
  val arrived = Value(2, "only can ping")
  val online = Value(3, "can ping and test ping")

  implicit val enumMapper = MappedColumnType.base[State, Int](_.id, this.apply)

  implicit val enumReads: Reads[State] = EnumUtils.enumReads(StateEnum)

  implicit def enumWrites: Writes[State] = EnumUtils.enumWrites

}
