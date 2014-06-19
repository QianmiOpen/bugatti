package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._

/**
 * 配置文件修改记录内容
 *
 * CREATE TABLE `sub_project_conf_log_content` (
 *  `id` INT            COMMENT '主键id',
 *  `content` TEXT      COMMENT '内容',
 *  PRIMARY KEY (`id`)
 * )
 *
 */
case class SubPConfLogContent(id: Int, content: String)
class SubPConfLogContentTable(tag: Tag) extends Table[SubPConfLogContent](tag, "sub_project_conf_log_content") {
  def id = column[Int]("id", O.PrimaryKey)
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
