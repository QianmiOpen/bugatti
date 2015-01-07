package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current


/**
 * Created by mind on 8/20/14.
 */
case class TemplateAlias(id: Option[Int],templateId: Option[Int], name: String, value: String, description: String, scriptVersion: String = ScriptVersionHelper.Master)

case class TemplateAliasTable(tag: Tag) extends Table[TemplateAlias](tag, "template_alias") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id")
  def name = column[String]("name")
  def value = column[String]("value")
  def description = column[String]("description")
  def scriptVersion = column[String]("script_version", O.Default(ScriptVersionHelper.Master))

  override def * = (id.?, templateId.?, name, value, description, scriptVersion) <> (TemplateAlias.tupled, TemplateAlias.unapply _)
}

object TemplateAliasHelper {
  import models.AppDB._

  val qTemplateAlias = TableQuery[TemplateAliasTable]

  def findByTemplateId_Version(templateId: Int, scriptVersion: String): Seq[TemplateAlias] = db withSession { implicit session =>
    qTemplateAlias.filter(t => t.templateId === templateId && t.scriptVersion === scriptVersion).list
  }

  def create(templateAlias: TemplateAlias): Int = db withSession { implicit session =>
    qTemplateAlias.returning(qTemplateAlias.map(_.id)).insert(templateAlias)
  }

  def deleteAliasByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTemplateAlias.filter(_.templateId === templateId).delete
  }

  def updateScriptVersion(oldVersion: String, newVersion: String) = db withSession { implicit session =>
    qTemplateAlias.filter(_.scriptVersion === oldVersion).map(_.scriptVersion).update(newVersion)
  }
}
