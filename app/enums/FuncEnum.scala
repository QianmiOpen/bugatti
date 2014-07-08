package enums

import play.api.libs.json.{Writes, Reads}

import scala.slick.driver.MySQLDriver.simple._
/**
 * 功能
 *
 * @author of546
 */
object FuncEnum extends Enumeration {
  type Func = Value
  val user = Value(1, "用户管理")
  val area = Value(2, "区域管理")
  val env = Value(3, "环境管理")
  val project = Value(4, "项目管理")
  val relation = Value(5, "关系配置")
  val task = Value(6, "任务管理")
  implicit val enumMapper = MappedColumnType.base[Func, Int](_.id, this.apply)

  implicit val enumReads: Reads[Func] = EnumUtils.enumReads(FuncEnum)

  implicit def enumWrites: Writes[Func] = EnumUtils.enumWrites
}
