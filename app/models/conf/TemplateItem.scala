package models.conf

import enums.ItemTypeEnum
import enums.ItemTypeEnum.ItemType
import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目模板描述表
 */
case class TemplateItem(id: Option[Int], templateId: Option[Int], itemName: String, itemDesc: Option[String], itemType: ItemType, default: Option[String], order: Int, scriptVersion: String = ScriptVersionHelper.Master) {
  override def equals(that: Any) = {
    that match {
      case ti: TemplateItem => ti.itemName == this.itemName
      case _ => false
    }
  }
}

class TemplateItemTable(tag: Tag) extends Table[TemplateItem](tag, "template_item") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def templateId = column[Int]("template_id")    // 模板编号
  def itemName = column[String]("item_name")     // 字段定义的名称
  def itemDesc = column[String]("item_desc", O.Nullable) // 字段定义的描述
  def itemType = column[ItemType]("item_type", O.DBType("enum('attr', 'var')"), O.Default(ItemTypeEnum.attribute))
  def default = column[String]("item_default", O.Nullable)
  def order = column[Int]("order", O.Default(0)) // 字段排序
  def scriptVersion = column[String]("script_version", O.Default(ScriptVersionHelper.Master), O.DBType("VARCHAR(60)"))

  override def * = (id.?, templateId.?, itemName, itemDesc.?, itemType, default.?, order, scriptVersion) <> (TemplateItem.tupled, TemplateItem.unapply _)
  def idx_order = index("idx_tid_order", (templateId, scriptVersion, order))
  def idx_name = index("idx_name", (templateId, itemName, scriptVersion), unique = true)
}
object TemplateItemHelper extends PlayCache {
  import models.AppDB._
  val qItem = TableQuery[TemplateItemTable]

  def findById(id: Int) = db withSession { implicit session =>
    qItem.filter(_.id === id).firstOption
  }

  def findByTemplateId(templateId: Int): Seq[TemplateItem] = db withSession { implicit session =>
    qItem.filter(_.templateId === templateId).sortBy(_.order).list
  }

  def findByTemplateId_ScriptVersion(templateId: Int, scriptVersion: String): Seq[TemplateItem] = db withSession { implicit session =>
    qItem.filter(t => t.templateId === templateId && t.scriptVersion === scriptVersion).sortBy(_.order).list
  }

  def findByItemType(templateId: Int, scriptVersion: String, itemType: ItemType): Seq[TemplateItem] = db withSession { implicit session =>
    val items = findByTemplateId_ScriptVersion(templateId, scriptVersion)
    items.filter(_.itemType == itemType)
  }

  def create(item: TemplateItem) = db withSession { implicit session =>
    _create(item)
  }

  def _create(item: TemplateItem)(implicit session: JdbcBackend#Session) = {
    qItem.returning(qItem.map(_.id)).insert(item)(session)
  }

  def _deleteByTemplateId(templateId: Int)(implicit session: JdbcBackend#Session) = {
    qItem.filter(_.templateId === templateId).delete
  }

  def update(id: Int, item: TemplateItem) = db withSession { implicit session =>
    val info2update = item.copy(Some(id))
    qItem.filter(_.id === id).update(info2update)
  }

  def deleteItemsByTemplateId(templateId: Int) = db withSession { implicit session =>
    qItem.filter(_.templateId === templateId).delete
  }

  def updateScriptVersion(oldVersion: String, newVersion: String) = db withSession { implicit session =>
    qItem.filter(_.scriptVersion === oldVersion).map(_.scriptVersion).update(newVersion)
  }
}
