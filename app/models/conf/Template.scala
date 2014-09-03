package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException
import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend
import scala.slick.lifted.ProvenShape

/**
 * 项目模板
 *
 * @author of546
 */
case class Template(id: Option[Int], name: String, remark: Option[String], dependentProjectIds: Seq[Int])
case class TemplateFrom(id: Option[Int], name: String, remark: Option[String], items: List[TemplateItem]) {
  def toTemplate = Template(id, name, remark, Seq.empty)
}

class TemplateTable(tag: Tag) extends Table[Template](tag, "template") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def remark = column[String]("remark", O.Nullable)
  def dependentProjectIds = column[Seq[Int]]("dependent_project", O.DBType("VARCHAR(254)"))(MappedColumnType.base[Seq[Int], String](
    _.mkString(","), _ match {
      case e if e.isEmpty => Seq.empty
      case x => x.split(",").map(_.toInt).toSeq
    }))

  override def *  = (id.?, name, remark.?, dependentProjectIds) <> (Template.tupled, Template.unapply _)
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
    val templateId = _create(template)
    items.map(item => TemplateItemHelper._create(item.copy(None, Some(templateId)))).size
  }

  @throws[UniqueNameException]
  def _create(template: Template)(implicit session: JdbcBackend#Session) = {
    try {
      qTemplate.returning(qTemplate.map(_.id)).insert(template)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
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
    items.map(item => TemplateItemHelper._create(item.copy(None, Some(id)))).size
  }

  @throws[UniqueNameException]
  def _update(id: Int, template: Template)(implicit session: JdbcBackend#Session) = {
    val template2update = template.copy(Some(id))
    try {
      qTemplate.filter(_.id === id).update(template2update)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

}
