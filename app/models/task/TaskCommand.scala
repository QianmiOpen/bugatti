package models.task

import play.api.libs.json.{JsValue, JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskCommand(id: Option[Int], taskId: Int, command: String, machine: String, sls: String, status: Int, orderNum: Int)

class TaskCommandTable(tag: Tag) extends Table[TaskCommand](tag, "task_command"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def taskId = column[Int]("task_id", O.NotNull)
  def command = column[String]("command", O.NotNull, O.DBType("VARCHAR(2000)"))
  def machine = column[String]("machine", O.NotNull, O.DBType("VARCHAR(100)"))
  def sls = column[String]("sls", O.NotNull, O.DBType("VARCHAR(100)"))
  def status = column[Int]("status", O.NotNull)
  def orderNum = column[Int]("order_num", O.NotNull)

  override def * = (id.?, taskId, command, machine, sls, status, orderNum) <> (TaskCommand.tupled, TaskCommand.unapply _)
}

object TaskCommandHelper{
  import models.AppDB._
  val qTaskCommand = TableQuery[TaskCommandTable]

  def addCommands(commands: Seq[TaskCommand]) = db withSession { implicit session =>
    qTaskCommand.insertAll(commands: _*)
  }

  def updateStatusByOrder(taskId: Int, orderNum: Int, status: Int) = db withSession{implicit session =>
    qTaskCommand.filter(_.taskId is taskId).filter(_.orderNum is orderNum)
      .map(command => command.status).update(status)
  }

}