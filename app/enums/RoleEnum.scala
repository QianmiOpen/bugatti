package enums

import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple._
/**
 * 用户角色
 *
 * @author of546
 */
object RoleEnum extends Enumeration {
  type Role = Value
  val admin = Value("admin") // 管理员
  val user = Value("user")   // 普通用户

  // implicit val enumMapper = MappedColumnType.base[role, Int](_.id, this.apply)
  //  implicit val enumMapper = MappedColumnType.base[role, String](r => r.toString, i => this.withName(i))
  implicit val enumMapper = MappedColumnType.base[Role, String](_.toString, this.withName(_))


  implicit val enumReads: Reads[Role] = EnumUtils.enumReads(RoleEnum)

  implicit def enumWrites: Writes[Role] = EnumUtils.enumWrites
}
