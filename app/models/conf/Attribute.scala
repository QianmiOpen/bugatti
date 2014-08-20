package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

import scala.slick.jdbc.JdbcBackend

/**
 * 项目属性
 */
case class Attribute(id: Option[Int], projectId: Option[Int], name: String, value: Option[String])

class AttributeTable(tag: Tag) extends Table[Attribute](tag, "attribute") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id")   // 项目编号
  def name = column[String]("name")   // 属性名称（同TemplateInfo.itemName)
  def value = column[String]("value", O.Nullable) // 属性值

  override def * = (id.?, projectId.?, name, value.?) <> (Attribute.tupled, Attribute.unapply _)
  def idx = index("idx_pid", projectId)
}

object AttributeHelper {
  import models.AppDB._
  val qAttribute = TableQuery[AttributeTable]

  def findByProjectId(projectId: Int): Seq[Attribute] = db withSession { implicit session =>
    qAttribute.filter(_.projectId === projectId).sortBy(_.id).list
  }

  def getValue(projectId: Int, name: String): Option[String] = {
    findByProjectId(projectId).find(_.name == name) match {
      case Some(attr) => attr.value
      case None => Option.empty[String]
    }
  }

  def _update(attribute: Attribute)(implicit session: JdbcBackend#Session) = {
    qAttribute.filter(_.id === attribute.id).update(attribute)
  }

  def _create(attribute: Attribute)(implicit session: JdbcBackend#Session) = {
    qAttribute.insert(attribute)
  }

  def _create(attr: Seq[Attribute])(implicit session: JdbcBackend#Session) = {
    qAttribute.insertAll(attr: _*)(session)
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qAttribute.filter(_.projectId === projectId).delete
  }
}