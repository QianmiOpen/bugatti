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
case class SubProject(id: Int, pid: Int, version: String, updated: Option[DateTime])
class SubProjectTable(tag: Tag) extends Table[SubProject](tag, "sub_project"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pid = column[Int]("pid", O.NotNull)   // 项目编号
  def version = column[String]("version", O.NotNull) // 当前版本
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id, pid, version, updated.?) <> (SubProject.tupled, SubProject.unapply _)
  def idx = index("idx_pid", pid)
}
object SubProjectHelper extends PlayCache {

  import models.AppDB._

  val qSubProject = TableQuery[SubProjectTable]

  def findById(id: Int) = db withSession { implicit session =>
    qSubProject.where(_.id is id).firstOption
  }

  def findByPid(pid: Int) = db withSession { implicit session =>
    qSubProject.where(_.pid is pid).list
  }

  def create(sp: SubProject) = db withSession { implicit session =>
    qSubProject.insert(sp)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qSubProject.where(_.id is id).delete
  }

  def update(id: Int, sp: SubProject) = db withSession { implicit session =>
    val sp2update = sp.copy(id)
    qSubProject.where(_.id is id).update(sp2update)
  }

}