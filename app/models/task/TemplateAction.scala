package models.task

import enums.ActionTypeEnum
import enums.ActionTypeEnum.ActionType
import models.conf.ScriptVersionHelper
import play.api.libs.json.Json
import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 17/6/14.
 */
case class TemplateAction (id: Option[Int], name: String, css: String, versionMenu: Boolean, typeId: Int, orderNum: Int, scriptVersion: String = ScriptVersionHelper.Master, actionType: ActionType = ActionTypeEnum.project)

class TemplateActionTable(tag: Tag) extends Table[TemplateAction](tag, "template_action"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.DBType("VARCHAR(64)"))
  def css = column[String]("css", O.DBType("VARCHAR(64)"))
  def versionMenu = column[Boolean]("version_menu", O.Default(false))
  def typeId = column[Int]("type_id")
  def orderNum = column[Int]("order_num")
  def scriptVersion = column[String]("script_version", O.Default(ScriptVersionHelper.Master), O.DBType("VARCHAR(60)"))
  def actionType = column[ActionType]("action_type", O.DBType(s"enum('${ActionTypeEnum.project}', '${ActionTypeEnum.host}')"), O.Default(ActionTypeEnum.project))

  override def * = (id.?, name, css, versionMenu, typeId, orderNum, scriptVersion, actionType) <> (TemplateAction.tupled, TemplateAction.unapply _)
}

object TemplateActionHelper{
  import models.AppDB._

  implicit val TaskTemplateReads = Json.format[TemplateAction]

  val qTaskTemplate = TableQuery[TemplateActionTable]

  def findById(tid: Int) = db withSession {implicit session =>
    qTaskTemplate.filter(_.id === tid).first
  }

  def findByScriptVerison(scriptVersion: String) = db withSession { implicit session =>
    qTaskTemplate.filter(_.scriptVersion === scriptVersion).list
  }

  def all = db withSession {implicit session =>
    qTaskTemplate.sortBy(x => (x.typeId, x.orderNum)).list
  }

  def create(template: TemplateAction) = db withSession { implicit session =>
    qTaskTemplate.returning(qTaskTemplate.map(_.id)).insert(template)
  }

  def create(templates: Seq[TemplateAction]) = db withSession { implicit session =>
    qTaskTemplate.insertAll(templates: _*)
  }

  def deleteTaskTemplateByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTaskTemplate.filter(_.typeId === templateId).delete
  }

  def findTaskTemplateByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTaskTemplate.filter(_.typeId === templateId).list
  }

  def updateScriptVersion(oldVersion: String, newVersion: String) = db withSession { implicit session =>
    qTaskTemplate.filter(_.scriptVersion === oldVersion).map(_.scriptVersion).update(newVersion)
  }
}
