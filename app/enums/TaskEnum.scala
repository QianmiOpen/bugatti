package enums

import play.api.libs.json._

import scala.slick.driver.MySQLDriver.simple._

/**
 * Created by jinwei on 4/7/14.
 */
object TaskEnum extends Enumeration {
  type TaskStatus = Value
  val TaskWait = Value(0) //"等待执行"
  val TaskSuccess = Value(1) //"执行成功"
  val TaskFailed = Value(2) //"执行失败"
  val TaskProcess = Value(3) //"正在执行"
  val TaskPass = Value(4) //"跳过执行"

  implicit val enumMapper = MappedColumnType.base[TaskStatus, Int](_.id, this.apply)

  implicit val enumReads: Reads[TaskStatus] = EnumUtils.enumReads(TaskEnum)

  implicit def enumWrites: Writes[TaskStatus] = EnumUtils.enumWritesNumber
}
