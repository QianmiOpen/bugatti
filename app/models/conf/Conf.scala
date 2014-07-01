package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * 子项目配置文件
 *
 * @author of546
 */
case class Conf(id: Int, eid: Int, pid: Int, vid: Int, name: String, path: String, lastUpdated: Option[DateTime])
class ConfTable(tag: Tag) extends Table[Conf](tag, "conf") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def eid = column[Int]("eid", O.NotNull)   // 环境编号
  def pid = column[Int]("pid", O.NotNull)   // 主项目编号
  def vid = column[Int]("vid", O.NotNull)   // 子项目编号
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(500)"))
  def lastUpdated= column[DateTime]("last_updated", O.Default(DateTime.now()))

  override def * = (id, eid, pid, vid, name, path, lastUpdated.?) <> (Conf.tupled, Conf.unapply _)

  def idx = index("idx_vid", (eid, vid))
}

object ConfHelper extends PlayCache {

  import models.AppDB._

  val qConf = TableQuery[ConfTable]

  def findById(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).firstOption
  }

  def findByPid(pid: Int) = db withSession { implicit session =>
    qConf.where(_.pid is pid).list
  }

  def create(conf: Conf) = db withSession { implicit session =>
    qConf.insert(conf)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).delete
  }

  def update(id: Int, conf: Conf) = db withSession { implicit session =>
    val conf2update = conf.copy(id)
    qConf.where(_.id is id).update(conf2update)
  }
}