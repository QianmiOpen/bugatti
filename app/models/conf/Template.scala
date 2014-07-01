package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目模板
 *
 * @author of546
 */
case class Template(id: Int, name: String)
class TemplateTable(tag: Tag) extends Table[Template](tag, "template") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  override def *  = (id, name) <> (Template.tupled, Template.unapply _)
  def idx = index("idx_name", name, unique = true)
}
object TemplateHelper extends PlayCache {

  import models.AppDB._

  val qTemplate = TableQuery[TemplateTable]

  def all = db withSession { implicit session =>
    qTemplate.list
  }

  def findById(id: Int) = db withSession { implicit session =>
    qTemplate.where(_.id is id).firstOption
  }

  def findByName(name: String) = db withSession { implicit session =>
    qTemplate.where(_.name is name).firstOption
  }

  def create(template: Template) = db withSession { implicit session =>
    qTemplate.insert(template)
  }

  def create(template: Template, infos: List[TemplateInfo]) = db withTransaction { implicit session =>
    val tid = create_(template)
    infos.foreach{ info =>
      val t = TemplateInfo(None, tid, info.itemName, info.itemDesc, info.order)
      TemplateInfoHelper.create_(t)
    }
  }

  def create_(template: Template)(implicit session: JdbcBackend#Session) = {
    qTemplate.returning(qTemplate.map(_.id)).insert(template)(session)
  }

  def update(id: Int, template: Template) = db withSession { implicit session =>
    val template2update = template.copy(id)
    qTemplate.where(_.id is id).update(template2update)
  }

}
