package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._

/**
 * 项目模板描述表
 */
case class TemplateInfo(id: Option[Int], tid: Int, itemName: String, itemDesc: String, order: Int)
class TemplateInfoTable(tag: Tag) extends Table[TemplateInfo](tag, "template_info") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def tid = column[Int]("tid") // 模板编号
  def itemName = column[String]("item_name", O.NotNull)
  def itemDesc = column[String]("item_desc", O.NotNull)
  def order = column[Int]("item_desc", O.NotNull, O.Default(0))

  override def * = (id.?, tid, itemName, itemDesc, order) <> (TemplateInfo.tupled, TemplateInfo.unapply _)
  def idx = index("idx_tid", tid)
  def idx_name = index("idx_name", (tid, itemName), unique = true)
}
object TemplateInfoHelper extends PlayCache {

  import models.AppDB._

  val qInfo = TableQuery[TemplateInfoTable]

  def findById(id: Int) = db withSession { implicit session =>
    qInfo.where(_.id is id).firstOption
  }

  def findByTid(tid: Int): List[TemplateInfo] = db withSession { implicit session =>
    qInfo.where(_.tid is tid).list
  }

  def create(templateInfo: TemplateInfo) = db withSession { implicit session =>
    qInfo.insert(templateInfo)
  }

  def update(id: Int, templateInfo: TemplateInfo) = db withSession { implicit session =>
    val info2update = templateInfo.copy(Some(id))
    qInfo.where(_.id is id).update(info2update)
  }

}
