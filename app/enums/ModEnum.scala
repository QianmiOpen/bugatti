package enums

import play.api.libs.json.{Writes, Reads}

/**
 * of546
 */
object ModEnum extends Enumeration {
  type Mod = Value
  val user = Value(1)
  val area = Value(2)
  val env = Value(3)
  val project = Value(4)
  val relation = Value(5)
  val task = Value(6)
  val conf = Value(7)
  val member = Value(8)
  val template = Value(9)
  val version = Value(10)
  val system = Value(11)
  val spirit = Value(12)

  implicit val enumReads: Reads[Mod] = EnumUtils.enumReads(ModEnum)

  implicit def enumWrites: Writes[Mod] = EnumUtils.enumWrites
}