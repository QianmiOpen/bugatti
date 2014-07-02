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
  def pid = column[Int]("pid", O.NotNull)   // 项目编号
  def name = column[String]("name", O.NotNull)   // 属性名称（同TemplateInfo.itemName)
  def value = column[String]("value", O.Nullable) // 属性值

  override def * = (id.?, pid.?, name, value.?) <> (Attribute.tupled, Attribute.unapply _)
  def idx = index("idx_pid", pid)
}

object AttributeHelper {
  import models.AppDB._
  val qAttribute = TableQuery[AttributeTable]

  def findByPid(pid: Int): List[Attribute] = db withSession { implicit session =>
    qAttribute.sortBy(_.id).where(_.pid is pid).list
  }

  def exists(typeId: Int, name: String): Boolean = {
    findByPid(typeId).filter(_.name == name).isEmpty
  }

  def create(attr: Attribute) = db withSession { implicit session =>
    qAttribute.insert(attr)
  }

  // batch
  def create(attr: List[Attribute])(implicit session: JdbcBackend#Session) = {
    qAttribute.insertAll(attr: _*)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qAttribute.where(_.id is id).delete
  }

  def deleteByPid_(pid: Int)(implicit session: JdbcBackend#Session) = {
    qAttribute.where(_.pid is pid).delete
  }

  def update(id: Int, attr: Attribute) = db withSession { implicit session =>
    val attr2update = attr.copy(Some(id))
    qAttribute.where(_.id is id).update(attr2update)
  }

}