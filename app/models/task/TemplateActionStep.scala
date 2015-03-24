package models.task

import play.api.libs.json.Json
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 18/6/14.
 */
case class TemplateActionStep(id: Option[Int], templateId: Int, name: String, sls: String, seconds: Int, orderNum: Int, doIf: Option[String])

class TemplateActionStepTable(tag: Tag) extends Table[TemplateActionStep](tag, "template_action_step") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id")
  def name = column[String]("name")
  def sls = column[String]("sls", O.DBType("VARCHAR(2000)"))
  def doIf = column[String]("do_if", O.Nullable, O.DBType("VARCHAR(2000)"))
  def seconds = column[Int]("seconds", O.Default(3))
  def orderNum = column[Int]("order_num")

  override def * = (id.?, templateId,name, sls, seconds, orderNum, doIf.?) <>(TemplateActionStep.tupled, TemplateActionStep.unapply _)
}

object TemplateActionStepHelper {

  import models.AppDB._

  implicit val TaskTemplateStepReads = Json.format[TemplateActionStep]

  val qTaskTemplateStep = TableQuery[TemplateActionStepTable]

  def findStepsByTemplateId(templateId: Int): Seq[TemplateActionStep] = db withSession { implicit session =>
    qTaskTemplateStep.filter(_.templateId === templateId).sortBy(r => r.orderNum).list
  }

  def create(action : TemplateActionStep) = db withSession {implicit session =>
    qTaskTemplateStep.returning(qTaskTemplateStep.map(_.id)).insert(action)
  }

  def deleteStepsByTaskTemplateId(stepTemplateId: Int) = db withSession { implicit session =>
    qTaskTemplateStep.filter(_.templateId === stepTemplateId).delete
  }
}