package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

import scala.slick.jdbc.JdbcBackend

/**
 * 子项目配置文件修改记录
 *
 * @author of546
 */
case class ConfLog(id: Option[Int], confId: Int, envId: Int, versionId: Int, jobNo: String, name: String, path: String, fileType: Option[String], remark: Option[String], updated: DateTime)

class ConfLogTable(tag: Tag) extends Table[ConfLog](tag, "conf_log") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def confId = column[Int]("conf_id")    // 配置文件编号
  def envId = column[Int]("env_id")     // 环境编号
  def versionId = column[Int]("version_id") // 版本编号
  def jobNo = column[String]("job_no", O.DBType("VARCHAR(16)"))
  def name = column[String]("file_name", O.DBType("VARCHAR(100)"))
  def path = column[String]("file_path", O.DBType("VARCHAR(200)"))
  def fileType = column[String]("file_type", O.Nullable, O.DBType("VARCHAR(50)"))
  def remark = column[String]("remark", O.Nullable, O.DBType("VARCHAR(500)")) // 回复的备注内容
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id.?, confId, envId, versionId, jobNo, name, path, fileType.?, remark.?, updated) <> (ConfLog.tupled, ConfLog.unapply _)

  def idx = index("idx_cid", (confId, updated))
}
object ConfLogHelper extends PlayCache {

  import models.AppDB._

  val qLog = TableQuery[ConfLogTable]

  def findById(id: Int): Option[ConfLog] = db withSession { implicit session =>
    qLog.filter(_.id === id).firstOption
  }

  def all(confId: Int, page: Int, pageSize: Int): Seq[ConfLog] = db withSession { implicit session =>
    val offset = pageSize * page
    qLog.filter(_.confId === confId).sortBy(_.updated desc).drop(offset).take(pageSize).list
  }

  def count(confId: Int) = db withSession { implicit session =>
    qLog.filter(_.confId === confId).length.run
  }

  def _create(log: ConfLog)(implicit session: JdbcBackend#Session) = {
    qLog.returning(qLog.map(_.id)).insert(log)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qLog.filter(_.id is id).delete
  }

  def update(id: Int, log: ConfLog) = db withSession { implicit session =>
    val log2update = log.copy(Some(id))
    qLog.filter(_.id === id).update(log2update)
  }

}