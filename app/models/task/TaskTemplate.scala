package models.task

import play.api.libs.json.Json
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 17/6/14.
 */
case class TaskTemplate (id: Option[Int], name: String, css: String, versionMenu: Boolean, typeId: Int, orderNum: Int)

class TaskTemplateTable(tag: Tag) extends Table[TaskTemplate](tag, "task_template"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.DBType("VARCHAR(64)"))
  def css = column[String]("css", O.DBType("VARCHAR(64)"))
  def versionMenu = column[Boolean]("version_menu", O.Default(false))
  def typeId = column[Int]("type_id")
  def orderNum = column[Int]("order_num")

  override def * = (id.?, name, css, versionMenu, typeId, orderNum) <> (TaskTemplate.tupled, TaskTemplate.unapply _)
}

object TaskTemplateHelper{
  import models.AppDB._

  implicit val TaskTemplateReads = Json.format[TaskTemplate]

  val qTaskTemplate = TableQuery[TaskTemplateTable]

  def findById(tid: Int) = db withSession {implicit session =>
    qTaskTemplate.filter(_.id === tid).first
  }

  def all = db withSession {implicit session =>
    qTaskTemplate.sortBy(x => (x.typeId, x.orderNum)).list()
  }

  def create(template: TaskTemplate) = db withSession { implicit session =>
    qTaskTemplate.returning(qTaskTemplate.map(_.id)).insert(template)
  }

  def create(templates: Seq[TaskTemplate]) = db withSession { implicit session =>
    qTaskTemplate.insertAll(templates: _*)
  }

  def deleteTaskTemplateByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTaskTemplate.filter(_.typeId === templateId).delete
  }

  def findTaskTemplateByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTaskTemplate.filter(_.typeId === templateId).list()
  }

}