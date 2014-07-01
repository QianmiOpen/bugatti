package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * 子项目配置文件修改记录
 *
 * @author of546
 */
case class ConfLog(id: Int, cid: Int, eid: Int, vid: Int, name: String, path: String, remark: Option[String], updated: Option[DateTime])

class ConfLogTable(tag: Tag) extends Table[ConfLog](tag, "conf_log") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def cid = column[Int]("cid", O.NotNull)   // 子项目配置文件编号
  def eid = column[Int]("eid", O.NotNull)     // 环境编号
  def vid = column[Int]("vid", O.NotNull)   // 子项目编号
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(500)"))
  def remark = column[String]("remark", O.NotNull, O.DBType("VARCHAR(500)")) // 回复的备注内容
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id, cid, eid, vid, name, path, remark.?, updated.?) <> (ConfLog.tupled, ConfLog.unapply _)
}
object ConfLogHelper extends PlayCache {

  import models.AppDB._

  val qLog = TableQuery[ConfLogTable]

  def findById(id: Int) = db withSession { implicit session =>
    qLog.where(_.id is id).firstOption
  }

  def create(log: ConfLog) = db withSession { implicit session =>
    qLog.insert(log)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qLog.where(_.id is id).delete
  }

  def update(id: Int, log: ConfLog) = db withSession { implicit session =>
    val log2update = log.copy(id)
    qLog.where(_.id is id).update(log2update)
  }
}