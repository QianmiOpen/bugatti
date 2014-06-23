package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._

/**
 * 子项目配置文件修改记录内容
 *
 * @author of546
 */
case class SubPConfLogContent(id: Int, content: String)
class SubPConfLogContentTable(tag: Tag) extends Table[SubPConfLogContent](tag, "sub_project_conf_log_content") {
  def id = column[Int]("id", O.PrimaryKey) // 子项目配置文件修改记录编号
  def content = column[String]("content", O.DBType("text"))

  override def * = (id, content) <> (SubPConfLogContent.tupled, SubPConfLogContent.unapply _)
}
object SubPConfLogContentHelper extends PlayCache {

  import models.AppDB._

  val qLogContent = TableQuery[SubPConfLogContentTable]

  def findById(id: Int) = db withSession { implicit session =>
    qLogContent.where(_.id is id).firstOption
  }

  def create(content: SubPConfLogContent) = db withSession { implicit session =>
    qLogContent.insert(content)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qLogContent.where(_.id is id).delete
  }

  def update(id: Int, content: SubPConfLogContent) = db withSession { implicit session =>
    val content2update = content.copy(id)
    qLogContent.where(_.id is id).update(content2update)
  }
}
