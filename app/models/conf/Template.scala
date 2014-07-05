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
    create_(template)
  }

  def create(template: Template, items: Seq[TemplateItem]) = db withTransaction { implicit session =>
    val tid = create_(template)

    items.map{ item =>
      val ti = TemplateItem(None, Some(tid), item.itemName, item.itemDesc, item.default, item.order)
      TemplateItemHelper.create_(ti)
    }.size
  }

  def create_(template: Template)(implicit session: JdbcBackend#Session) = {
    qTemplate.returning(qTemplate.map(_.id)).insert(template)(session)
  }

  def update(id: Int, template: Template) = db withSession { implicit session =>
    update_(id, template)
  }

  def update_(id: Int, template: Template)(implicit session: JdbcBackend#Session) = {
    val template2update = template.copy(Some(id))
    qTemplate.where(_.id is id).update(template2update)(session)
  }

  def update(id: Int, template: Template, items: List[TemplateItem]) = db withTransaction { implicit session =>
    update_(id, template) // 更新项目
    TemplateItemHelper.deleteByTid_(id) // 删除该项目下所有属性

    items.map{ item =>  // 插入新属性
      val ti = TemplateItem(None, Some(id), item.itemName, item.itemDesc, item.default, item.order)
      TemplateItemHelper.create_(ti)
    }.size

  }

  def delete_(id: Int)(implicit session: JdbcBackend#Session) = {
    qTemplate.where(_.id is id).delete
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    delete_(id)(session)
    TemplateItemHelper.deleteByTid_(id)
  }

}
