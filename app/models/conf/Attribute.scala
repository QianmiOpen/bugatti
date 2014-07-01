package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
/**
 * 项目属性
 */
case class Attribute(id: Option[Int], pid: Int, key: String, value: String)

class AttributeTable(tag: Tag) extends Table[Attribute](tag, "attribute") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pid = column[Int]("pid", O.NotNull)   // 项目编号
  def key = column[String]("key", O.NotNull)
  def value = column[String]("value", O.NotNull)

  override def * = (id.?, pid, key, value) <> (Attribute.tupled, Attribute.unapply _)
  def idx = index("idx_pid", pid)
}

object AttributeHelper {
  import models.AppDB._
  val qAttribute = TableQuery[AttributeTable]

  def findByPid(pid: Int): List[Attribute] = db withSession { implicit session =>
    qAttribute.sortBy(_.id).where(_.pid is pid).list
  }

  def exists(typeId: Int, name: String): Boolean = {
    findByPid(typeId).filter(_.key == name).isEmpty
  }

  def create(attr: Attribute) = db withSession { implicit session =>
    qAttribute.insert(attr)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qAttribute.where(_.id is id).delete
  }

  def update(id: Int, attr: Attribute) = db withSession { implicit session =>
    val attr2update = attr.copy(Some(id))
    qAttribute.where(_.id is id).update(attr2update)
  }

}