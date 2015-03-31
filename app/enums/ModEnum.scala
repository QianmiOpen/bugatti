package enums

import play.api.libs.json.{Writes, Reads}

/**
 * of546
 */
object ModEnum extends Enumeration {
  type Mod = Value
  val user = Value(1)     // 用户管理
  val area = Value(2)     // 区域管理
  val env = Value(3)      // 环境管理
  val project = Value(4)  // 项目管理(+属性\变量)
  val relation = Value(5) // 关系设置
  val task = Value(6)     // 任务管理(+负载)
  val conf = Value(7)     // 项目配置
  val member = Value(8)   // 成员管理(=项目\环境)
  val template = Value(9) // 项目模板
  val version = Value(10) // 项目版本
  val system = Value(11)  // 系统配置
  val spirit = Value(12)  // 网关管理
  val script = Value(13)  // 脚本设置
  val depend = Value(14)  // 项目依赖

  implicit val enumReads: Reads[Mod] = EnumUtils.enumReads(ModEnum)

  implicit def enumWrites: Writes[Mod] = EnumUtils.enumWrites
}