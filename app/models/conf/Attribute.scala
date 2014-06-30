package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
/**
 * 项目类型属性
 */
case class Attribute(id: Option[Int], typeId: Int, key: String, value: String)

class AttributeTable(tag: Tag) extends Table[Attribute](tag, "attribute") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def typeId = column[Int]("type_id", O.NotNull)  // 项目类型编号
  def key = column[String]("name", O.NotNull)
  def value = column[String]("value", O.NotNull)

  override def * = (id.?, typeId, key, value) <> (Attribute.tupled, Attribute.unapply _)
  def idx = index("idx_type_id", typeId)
  def keyx = index("idx_tid_key", (typeId, key), unique = true)
}

object AttributeHelper {
  import models.AppDB._
  val qAttribute = TableQuery[AttributeTable]

  def findByTypeId(typeId: Int): List[Attribute] = db withSession { implicit session =>
    qAttribute.sortBy(_.id).where(_.typeId is typeId).list
  }

  def exists(typeId: Int, name: String): Boolean = {
    findByTypeId(typeId).filter(_.key == name).isEmpty
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