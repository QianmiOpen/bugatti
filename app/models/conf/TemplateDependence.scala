package models.conf

import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by mind on 10/11/14.
 */

case class TemplateDependence(id: Option[Int], templateId: Int, name: String, description: String, defaultId: Int)
class TemplateDependenceTable(tag: Tag) extends Table[TemplateDependence](tag, "template_dependence") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id", O.Nullable)
  def name = column[String]("name", O.Nullable, O.DBType("VARCHAR(64)"))
  def description = column[String]("description", O.DBType("VARCHAR(128)"))
  def defaultId = column[Int]("default_id", O.Nullable)

  override def * = (id.?, templateId, name, description, defaultId) <> (TemplateDependence.tupled, TemplateDependence.unapply _)

  def idx = index("idx_tempid_name", (templateId, name), unique = true)
}

object TemplateDependenceHelper extends PlayCache {
  import models.AppDB._

  val qTemplateDependence = TableQuery[TemplateDependenceTable]

  def listByTemplateId(templateId: Int) = db withSession { implicit session =>
    qTemplateDependence.filter(t => t.templateId === templateId).list
  }

  def create(templateDep: TemplateDependence):Option[Int] = db withSession { implicit session =>
    qTemplateDependence.filter(x => x.templateId === templateDep.templateId && x.name === templateDep.name).firstOption.map{
      case x: TemplateDependence => x.id.get
      case None => {
        qTemplateDependence.returning(qTemplateDependence.map(_.id)).insert(templateDep)
      }
    }
  }
}
