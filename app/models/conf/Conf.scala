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
case class Conf(id: Option[Int], eid: Int, pid: Int, vid: Int, name: String, path: String, remark: Option[String], updated: DateTime)
case class ConfForm(id: Option[Int], eid: Int, pid: Int, vid: Int, name: Option[String], path: String, content: String, remark: Option[String], updated: DateTime) {
  def toConf = Conf(id, eid, pid, vid, path.substring(path.lastIndexOf("/") + 1), path, remark, updated)
}
class ConfTable(tag: Tag) extends Table[Conf](tag, "conf") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def eid = column[Int]("eid", O.NotNull)   // 环境编号
  def pid = column[Int]("pid", O.NotNull)   // 主项目编号
  def vid = column[Int]("vid", O.NotNull)   // 子项目编号
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(500)"))
  def remark = column[String]("remark", O.Nullable, O.DBType("VARCHAR(500)")) // 回复的备注内容
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id.?, eid, pid, vid, name, path, remark.?, updated) <> (Conf.tupled, Conf.unapply _)

  def idx_vid = index("idx_vid", vid)
  def idx = index("idx_eid_vid", (eid, vid, updated))
}

object ConfHelper extends PlayCache {

  import models.AppDB._

  val qConf = TableQuery[ConfTable]

  def findById(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).firstOption
  }

  def findByVid(vid: Int): List[Conf] = db withSession { implicit session =>
    qConf.where(_.vid is vid).list
  }

  def findByEid_Vid(eid: Int, vid: Int): List[Conf] = db withSession { implicit session =>
    qConf.sortBy(_.updated desc).where(c => c.eid === eid && c.vid === vid).list
  }

  def create(confForm: ConfForm) = db withTransaction { implicit session =>
    val id = qConf.returning(qConf.map(_.id)).insert(confForm.toConf)
    val content = ConfContent(Some(id), confForm.content)
    ConfContentHelper.create_(content)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qConf.where(_.id is id).delete
  }

  def update(id: Int, conf: Conf) = db withSession { implicit session =>
    val conf2update = conf.copy(Some(id))
    qConf.where(_.id is id).update(conf2update)
  }
}