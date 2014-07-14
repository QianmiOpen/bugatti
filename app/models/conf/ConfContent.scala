package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 子项目配置文件内容
 *
 * @author of546
 */
case class ConfContent(id: Option[Int], content: String)
class ConfContentTable(tag: Tag) extends Table[ConfContent](tag, "conf_content") {
  def id = column[Int]("conf_id", O.PrimaryKey) // 版本配置文件编号
  def content = column[String]("content", O.DBType("text"))

  override def * = (id.?, content) <> (ConfContent.tupled, ConfContent.unapply _)
}
object ConfContentHelper extends PlayCache {

  import models.AppDB._

  val qConfContent = TableQuery[ConfContentTable]

  def findById(id: Int): Option[ConfContent] = db withSession { implicit session =>
    qConfContent.filter(_.id === id).firstOption
  }

  def _create(content: ConfContent)(implicit session: JdbcBackend#Session) = {
    qConfContent.insert(content)(session)
  }

  def _delete(id: Int)(implicit session: JdbcBackend#Session) = {
    qConfContent.filter(_.id === id).delete(session)
  }

  def _update(id: Int, content: ConfContent)(implicit session: JdbcBackend#Session) = {
    val content2update = content.copy(Some(id))
    qConfContent.filter(_.id === id).update(content2update)(session)
  }

}