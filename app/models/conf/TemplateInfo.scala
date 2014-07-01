package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目模板描述表
 */
case class TemplateInfo(id: Option[Int], tid: Int, itemName: String, itemDesc: String, order: Int)
class TemplateInfoTable(tag: Tag) extends Table[TemplateInfo](tag, "template_info") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def tid = column[Int]("tid") // 模板编号
  def itemName = column[String]("item_name", O.NotNull) // 字段定义的名称
  def itemDesc = column[String]("item_desc", O.NotNull) // 字段定义的描述
  def order = column[Int]("item_desc", O.NotNull, O.Default(0)) // 字段排序

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
    create_(templateInfo)
  }

  def create_(templateInfo: TemplateInfo)(implicit session: JdbcBackend#Session) = {
    qInfo.insert(templateInfo)(session)
  }

  def update(id: Int, templateInfo: TemplateInfo) = db withSession { implicit session =>
    val info2update = templateInfo.copy(Some(id))
    qInfo.where(_.id is id).update(info2update)
  }

}
