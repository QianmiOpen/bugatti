package models.task

import play.api.libs.json.Json
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskTemplateStep(id: Option[Int], templateId: Int, name: String, sls: String, seconds: Int, orderNum: Int)

class TaskTemplateStepTable(tag: Tag) extends Table[TaskTemplateStep](tag, "task_template_step") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id")
  def name = column[String]("name")
  def sls = column[String]("sls", O.DBType("VARCHAR(2000)"))
  def seconds = column[Int]("seconds", O.Default(3))
  def orderNum = column[Int]("order_num")

  override def * = (id.?, templateId,name, sls, seconds, orderNum) <>(TaskTemplateStep.tupled, TaskTemplateStep.unapply _)
}

object TaskTemplateStepHelper {

  import models.AppDB._

  implicit val TaskTemplateStepReads = Json.format[TaskTemplateStep]

  val qTaskTemplateStep = TableQuery[TaskTemplateStepTable]

  def findStepsByTemplateId(templateId: Int): Seq[TaskTemplateStep] = db withSession { implicit session =>
    qTaskTemplateStep.where(_.templateId is templateId).sortBy(r => r.orderNum).list
  }

  def create(action : TaskTemplateStep) = db withSession {implicit session =>
    qTaskTemplateStep.returning(qTaskTemplateStep.map(_.id)).insert(action)
  }
}