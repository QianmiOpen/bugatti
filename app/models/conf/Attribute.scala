package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

import scala.slick.jdbc.JdbcBackend

/**
 * 项目属性
 */
case class Attribute(id: Option[Int], pid: Option[Int], name: String, value: Option[String])

class AttributeTable(tag: Tag) extends Table[Attribute](tag, "attribute") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id", O.NotNull)   // 项目编号
  def name = column[String]("name", O.NotNull)   // 属性名称（同TemplateInfo.itemName)
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
    findByProjectId(projectId).filter(_.name == name)(0).value
  }

  def _create(attr: List[Attribute])(implicit session: JdbcBackend#Session) = {
    qAttribute.insertAll(attr: _*)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qAttribute.filter(_.id === id).delete
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qAttribute.filter(_.projectId === projectId).delete
  }

  def update(id: Int, attr: Attribute) = db withSession { implicit session =>
    val attr2update = attr.copy(Some(id))
    qAttribute.filter(_.id === id).update(attr2update)
  }

}