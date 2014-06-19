package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._


/**
 * 子项目配置文件
 */
case class SubPConf(id: Int, eid: Int, pid: Int, spid: Int, name: String, path: String, lastUpdated: Option[DateTime])
class SubPConfTable(tag: Tag) extends Table[SubPConf](tag, "sub_project_conf") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def eid = column[Int]("eid", O.NotNull)
  def pid = column[Int]("pid", O.NotNull)
  def spid = column[Int]("spid", O.NotNull)
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(500)"))
  def lastUpdated= column[DateTime]("last_updated", O.Default(DateTime.now()))

  override def * = (id, eid, pid, spid, name, path, lastUpdated.?) <> (SubPConf.tupled, SubPConf.unapply _)
}
object SubPConfHelper extends PlayCache {

  import models.AppDB._

  val qConf = TableQuery[SubPConfTable]

  def findById(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).firstOption
  }

  def findByPid(pid: Int) = db withSession { implicit session =>
    qConf.where(_.pid is pid).list
  }

  def create(spconf: SubPConf) = db withSession { implicit session =>
    qConf.insert(spconf)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).delete
  }

  def update(id: Int, conf: SubPConf) = db withSession { implicit session =>
    val conf2update = conf.copy(id)
    qConf.where(_.id is id).update(conf2update)
  }
}