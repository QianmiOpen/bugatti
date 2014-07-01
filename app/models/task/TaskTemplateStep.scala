package models.task

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 18/6/14.
 */
case class TaskTemplateStep (id: Option[Int], templateId: Int, sls: String, orderNum: Int)

class TaskTemplateStepTable(tag: Tag) extends Table[TaskTemplateStep](tag, "task_template_step"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id", O.NotNull)
  def sls = column[String]("sls", O.NotNull, O.DBType("VARCHAR(100)"))
  def orderNum = column[Int]("order_num", O.NotNull)

  override def * = (id.?, templateId, sls, orderNum) <> (TaskTemplateStep.tupled, TaskTemplateStep.unapply _)
}

object TaskTemplateStepHelper{
  import models.AppDB._

  implicit val TaskTemplateStepReads: Reads[TaskTemplateStep] = (
    (JsPath \ "id").readNullable[Int] and
    (JsPath \ "templateId").read[Int] and
    (JsPath \ "sls").read[String] and
    (JsPath \ "orderNum").read[Int]
    )(TaskTemplateStep.apply _)

  val qTaskTemplateStep = TableQuery[TaskTemplateStepTable]

  def getStepsByTemplateId(templateId: Int): Seq[TaskTemplateStep] = db withSession {implicit session =>
    qTaskTemplateStep.where(_.templateId is templateId).sortBy(r => r.orderNum).list
  }
}