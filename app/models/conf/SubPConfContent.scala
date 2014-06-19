package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
/**
 * 配置文件内容
 */
case class SubPConfContent(id: Int, content: String)
class SubPConfContentTable(tag: Tag) extends Table[SubPConfContent](tag, "sub_project_conf_content") {
  def id = column[Int]("id", O.PrimaryKey)
  def content = column[String]("content", O.DBType("text"))

  override def * = (id, content) <> (SubPConfContent.tupled, SubPConfContent.unapply _)
}
object SubPConfContentHelper extends PlayCache {

  import models.AppDB._

  val qConfContent = TableQuery[SubPConfContentTable]

  def findById(id: Int) = db withSession { implicit session =>
    qConfContent.where(_.id is id).firstOption
  }

  def create(content: SubPConfContent) = db withSession { implicit session =>
    qConfContent.insert(content)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qConfContent.where(_.id is id).delete
  }

  def update(id: Int, content: SubPConfContent) = db withSession { implicit session =>
    val content2update = content.copy(id)
    qConfContent.where(_.id is id).update(content2update)
  }

}