package enums

import play.api.libs.json._
import scala.slick.driver.MySQLDriver.simple._

/**
 * 容器类型
 *
 * @author of546
 */
object ContainerTypeEnum extends Enumeration {
  type Container = Value
  val vm = Value("vm")
  val docker = Value("docker")

  implicit val enumMapper = MappedColumnType.base[Container, String](_.toString, this.withName(_))

  implicit val enumReads: Reads[Container] = EnumUtils.enumReads(ContainerTypeEnum)

  implicit def enumWrites: Writes[Container] = EnumUtils.enumWrites
}
