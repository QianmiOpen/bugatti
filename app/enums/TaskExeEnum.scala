package enums

import play.api.libs.json._

import scala.slick.driver.MySQLDriver.simple._
/**
 * Created by jinwei on 11/2/15.
 */
object TaskExeEnum extends Enumeration{
  type TaskExeWay = Value
  val TaskExeJudge = Value(0)//判断执行
  val TaskExeForce = Value(1)//强制执行

  implicit val enumMapper = MappedColumnType.base[TaskExeWay, Int](_.id, this.apply)

  implicit val enumReads: Reads[TaskExeWay] = EnumUtils.enumReads(TaskExeEnum)

  implicit val enumWrites: Writes[TaskExeWay] = EnumUtils.enumWritesNumber
}
