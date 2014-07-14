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
case class Conf(id: Option[Int], eid: Int, pid: Int, vid: Int, jobNo: String, name: String, path: String, remark: Option[String], updated: DateTime)
case class ConfForm(id: Option[Int], eid: Int, pid: Int, vid: Int, jobNo: String, name: Option[String], path: String, content: String, remark: Option[String], updated: DateTime) {
  def toConf = Conf(id, eid, pid, vid, jobNo, path.substring(path.lastIndexOf("/") + 1), path, remark, updated)
}
class ConfTable(tag: Tag) extends Table[Conf](tag, "conf") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.NotNull)     // 环境编号
  def projectId = column[Int]("project_id", O.NotNull) // 项目编号
  def versionId = column[Int]("version_id", O.NotNull) // 项目版本编号
  def jobNo = column[String]("job_no", O.NotNull, O.DBType("VARCHAR(16)"))
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(50)"))
  def path = column[String]("path", O.NotNull, O.DBType("VARCHAR(200)"))
  def remark = column[String]("remark", O.Nullable, O.DBType("VARCHAR(500)")) // 回复的备注内容
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id.?, envId, projectId, versionId, jobNo, name, path, remark.?, updated) <> (Conf.tupled, Conf.unapply _)

  def idx_vid = index("idx_vid", versionId)
  def idx_path = index("idx_path", (envId, versionId, path), unique = true)
  def idx = index("idx_eid_vid", (envId, versionId, updated))
}

object ConfHelper extends PlayCache {

  import models.AppDB._

  val qConf = TableQuery[ConfTable]

  def findById(id: Int): Option[Conf] = db withSession { implicit session =>
    qConf.filter(_.id === id).firstOption
  }

  def findByVersionId(versionId: Int): Seq[Conf] = db withSession { implicit session =>
    qConf.filter(_.versionId === versionId).list
  }

  def findByEnvId_VersionId(envId: Int, versionId: Int): Seq[Conf] = db withSession { implicit session =>
    qConf.filter(c => c.envId === envId && c.versionId === versionId).sortBy(_.updated desc).list
  }

  def findByEnvId_ProjectId_VersionId(envId: Int, projectId: Int, versionId: Int): Seq[Conf] = db withSession { implicit session =>
    qConf.filter(c => c.envId === envId && c.projectId === projectId && c.versionId === versionId).list
  }

  def create(confForm: ConfForm) = db withTransaction { implicit session =>
    val id = qConf.returning(qConf.map(_.id)).insert(confForm.toConf)
    val content = ConfContent(Some(id), confForm.content)
    ConfContentHelper._create(content)
  }

  def delete(conf: Conf) = db withTransaction { implicit session =>
    qConf.filter(_.id === conf.id).delete
    ConfContentHelper._delete(conf.id.get)
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    findById(id) match {
      case Some(conf) =>
        val confLogId = ConfLogHelper._create(ConfLog(None, id, conf.eid, conf.vid, conf.jobNo, conf.name, conf.path, conf.remark, conf.updated))
        qConf.filter(_.id === id).delete
        ConfContentHelper.findById(id) match {
          case Some(content) =>
            ConfLogContentHelper._create(ConfLogContent(confLogId, content.content))
            ConfContentHelper._delete(id)
          case None =>
            -2
        }
      case None =>
        -1
    }
  }

  def update(id: Int, confForm: ConfForm) = db withSession { implicit session =>
    findById(id) match {
      case Some(conf) =>
        val confLogId = ConfLogHelper._create(ConfLog(None, id, conf.eid, conf.vid, conf.jobNo, conf.name, conf.path, conf.remark, conf.updated))
        qConf.filter(_.id === id).update(confForm.toConf.copy(Some(id)))
        ConfContentHelper.findById(id) match {
          case Some(content) =>
            ConfLogContentHelper._create(ConfLogContent(confLogId, content.content))
            ConfContentHelper._update(id, ConfContent(None, confForm.content))
          case None =>
            -2
        }
      case None =>
        -1
    }
  }

}