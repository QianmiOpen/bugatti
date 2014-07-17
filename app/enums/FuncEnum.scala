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
  val user = Value(1, "user")         // 用户管理
  val area = Value(2, "area")         // 区域管理
  val env = Value(3, "env")           // 环境管理
  val project = Value(4, "project")   // 项目管理
  val relation = Value(5, "relation") // 关系配置
  val task = Value(6, "task")         // 任务管理
  implicit val enumMapper = MappedColumnType.base[Func, Int](_.id, this.apply)

  implicit val enumReads: Reads[Func] = EnumUtils.enumReads(FuncEnum)

  implicit def enumWrites: Writes[Func] = EnumUtils.enumWrites

}
object ModEnum extends Enumeration {
  val user, area, env, project, relation, task, conf, member, template, version = Value
}