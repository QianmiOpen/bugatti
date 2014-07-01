package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * 子项目
 *
 * @author of546
 */
case class Version(id: Int, pid: Int, version: String, updated: Option[DateTime])
class VersionTable(tag: Tag) extends Table[Version](tag, "version"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pid = column[Int]("pid", O.NotNull)   // 项目编号
  def vs = column[String]("version", O.NotNull) // 当前版本
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id, pid, vs, updated.?) <> (Version.tupled, Version.unapply _)
  def idx = index("idx_pid", pid)
  def idx_vs = index("idx_pid_vs", (pid, vs), unique = true)
}
object VersionHelper extends PlayCache {

  import models.AppDB._

  val qVersion = TableQuery[VersionTable]

  def findById(id: Int) = db withSession { implicit session =>
    qVersion.where(_.id is id).firstOption
  }

  def findByPid(pid: Int) = db withSession { implicit session =>
    qVersion.where(_.pid is pid).list
  }

  def create(sp: Version) = db withSession { implicit session =>
    qVersion.insert(sp)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qVersion.where(_.id is id).delete
  }

  def update(id: Int, sp: Version) = db withSession { implicit session =>
    val sp2update = sp.copy(id)
    qVersion.where(_.id is id).update(sp2update)
  }

}