package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 子项目配置文件修改记录内容
 *
 * @author of546
 */
case class ConfLogContent(id: Int, octet: Boolean, content: Array[Byte])
class ConfLogContentTable(tag: Tag) extends Table[ConfLogContent](tag, "conf_log_content") {
  def id = column[Int]("conf_log_id", O.PrimaryKey) // 版本配置文件修改记录编号
  def octet = column[Boolean]("octet", O.Default(false))
  def content = column[Array[Byte]]("content", O.DBType("MEDIUMBLOB"))

  override def * = (id, octet, content) <> (ConfLogContent.tupled, ConfLogContent.unapply _)
}
object ConfLogContentHelper extends PlayCache {

  import models.AppDB._

  val qLogContent = TableQuery[ConfLogContentTable]

  def findById(id: Int) = db withSession { implicit session =>
    qLogContent.filter(_.id === id).firstOption
  }

  def _create(content: ConfLogContent)(implicit session: JdbcBackend#Session) = {
    qLogContent.insert(content)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qLogContent.filter(_.id === id).delete
  }

  def update(id: Int, content: ConfLogContent) = db withSession { implicit session =>
    val content2update = content.copy(id)
    qLogContent.filter(_.id === id).update(content2update)
  }
}
