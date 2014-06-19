package enums

import scala.slick.driver.MySQLDriver.simple._
/**
 * 角色
 */
object RoleEnum extends Enumeration {
  type Role = Value
  val admin = Value("admin")
  val user = Value("user")

  // implicit val enumMapper = MappedColumnType.base[role, Int](_.id, this.apply)
  //  implicit val enumMapper = MappedColumnType.base[role, String](r => r.toString, i => this.withName(i))
  implicit val enumMapper = MappedColumnType.base[Role, String](_.toString, this.withName(_))
}
