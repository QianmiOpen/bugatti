package models.task

import enums.TaskEnum.TaskStatus
import play.api.libs.json.{JsValue, JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskCommand(id: Option[Int], taskId: Int, command: String, machine: String, sls: String, status: TaskStatus, orderNum: Int)

class TaskCommandTable(tag: Tag) extends Table[TaskCommand](tag, "task_command"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def taskId = column[Int]("task_id")
  def command = column[String]("command", O.DBType("VARCHAR(2000)"))
  def machine = column[String]("machine", O.DBType("VARCHAR(100)"))
  def sls = column[String]("sls", O.DBType("VARCHAR(100)"))
  def status = column[TaskStatus]("status")
  def orderNum = column[Int]("order_num")

  override def * = (id.?, taskId, command, machine, sls, status, orderNum) <> (TaskCommand.tupled, TaskCommand.unapply _)
}

object TaskCommandHelper{
  import models.AppDB._
  val qTaskCommand = TableQuery[TaskCommandTable]

  def create(commands: Seq[TaskCommand]) = db withSession { implicit session =>
    qTaskCommand.insertAll(commands: _*)
  }

  def update(taskId: Int, orderNum: Int, status: TaskStatus) = db withSession{implicit session =>
    qTaskCommand.filter(tc => tc.taskId === taskId && tc.orderNum === orderNum)
      .map(command => command.status).update(status)
  }

}