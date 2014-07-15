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
case class Template(id: Option[Int], name: String, remark: Option[String])
case class TemplateFrom(id: Option[Int], name: String, remark: Option[String], items: List[TemplateItem]) {
  def toTemplate = Template(id, name, remark)
}
class TemplateTable(tag: Tag) extends Table[Template](tag, "template") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def remark = column[String]("remark", O.Nullable)

  override def *  = (id.?, name, remark.?) <> (Template.tupled, Template.unapply _)
  def idx = index("idx_name", name, unique = true)
}
object TemplateHelper extends PlayCache {

  import models.AppDB._

  val qTemplate = TableQuery[TemplateTable]

  def findById(id: Int) = db withSession { implicit session =>
    qTemplate.filter(_.id === id).firstOption
  }

  def findByName(name: String) = db withSession { implicit session =>
    qTemplate.filter(_.name === name).firstOption
  }

  def all = db withSession { implicit session =>
    qTemplate.list
  }

  def create(template: Template) = db withSession { implicit session =>
    _create(template)
  }

  def create(template: Template, items: Seq[TemplateItem]) = db withTransaction { implicit session =>
    val tid = _create(template)
    items.map{ item =>
      val ti = TemplateItem(None, Some(tid), item.itemName, item.itemDesc, item.default, item.order)
      TemplateItemHelper._create(ti)
    }.size
  }

  def _create(template: Template)(implicit session: JdbcBackend#Session) = {
    qTemplate.returning(qTemplate.map(_.id)).insert(template)(session)
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    _delete(id)(session)
    TemplateItemHelper._deleteByTemplateId(id)
  }

  def _delete(id: Int)(implicit session: JdbcBackend#Session) = {
    qTemplate.filter(_.id === id).delete
  }

  def update(id: Int, template: Template) = db withSession { implicit session =>
    _update(id, template)
  }

  def update(id: Int, template: Template, items: Seq[TemplateItem]) = db withTransaction { implicit session =>
    _update(id, template) // 更新项目
    TemplateItemHelper._deleteByTemplateId(id) // 删除该项目下所有属性
    items.map{ item =>  // 插入新属性
      val ti = TemplateItem(None, Some(id), item.itemName, item.itemDesc, item.default, item.order)
      TemplateItemHelper._create(ti)
    }.size
  }

  def _update(id: Int, template: Template)(implicit session: JdbcBackend#Session) = {
    val template2update = template.copy(Some(id))
    qTemplate.filter(_.id === id).update(template2update)(session)
  }

}
