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
case class SubPConfLog(id: Int, scid: Int, eid: Int, spid: Int, name: String, path: String, remark: Option[String], updated: Option[DateTime])

class SubPConfLogTable(tag: Tag) extends Table[SubPConfLog](tag, "sub_project_conf_log") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def scid = column[Int]("scid", O.NotNull)   // 子项目配置文件编号
  def eid = column[Int]("eid", O.NotNull)     // 环境编号
  def spid = column[Int]("spid", O.NotNull)   // 子项目编号
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(500)"))
  def remark = column[String]("remark", O.NotNull, O.DBType("VARCHAR(500)")) // 回复的备注内容
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id, scid, eid, spid, name, path, remark.?, updated.?) <> (SubPConfLog.tupled, SubPConfLog.unapply _)
}
object SubPConfLogHelper extends PlayCache {

  import models.AppDB._

  val qLog = TableQuery[SubPConfLogTable]

  def findById(id: Int) = db withSession { implicit session =>
    qLog.where(_.id is id).firstOption
  }

  def create(log: SubPConfLog) = db withSession { implicit session =>
    qLog.insert(log)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qLog.where(_.id is id).delete
  }

  def update(id: Int, log: SubPConfLog) = db withSession { implicit session =>
    val log2update = log.copy(id)
    qLog.where(_.id is id).update(log2update)
  }
}