package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
/**
 * 子项目配置文件内容
 *
 * @author of546
 */
case class ConfContent(id: Int, content: String)
class ConfContentTable(tag: Tag) extends Table[ConfContent](tag, "conf_content") {
  def id = column[Int]("id", O.PrimaryKey) // 子项目配置文件编号
  def content = column[String]("content", O.DBType("text"))

  override def * = (id, content) <> (ConfContent.tupled, ConfContent.unapply _)
}
object ConfContentHelper extends PlayCache {

  import models.AppDB._

  val qConfContent = TableQuery[ConfContentTable]

  def findById(id: Int) = db withSession { implicit session =>
    qConfContent.where(_.id is id).firstOption
  }

  def create(content: ConfContent) = db withSession { implicit session =>
    qConfContent.insert(content)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qConfContent.where(_.id is id).delete
  }

  def update(id: Int, content: ConfContent) = db withSession { implicit session =>
    val content2update = content.copy(id)
    qConfContent.where(_.id is id).update(content2update)
  }

}