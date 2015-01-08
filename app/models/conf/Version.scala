package models.conf

import com.github.tototoshi.slick.MySQLJodaSupport._
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import enums.LevelEnum
import exceptions.UniqueNameException
import models.PlayCache
import org.joda.time.DateTime
import play.api.Play.current

import scala.language.postfixOps
import scala.slick.driver.MySQLDriver.simple._

/**
 * 子项目
 *
 * @author of546
 */
case class Version(id: Option[Int], projectId: Int, vs: String, updated: DateTime)
class VersionTable(tag: Tag) extends Table[Version](tag, "version"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id", O.NotNull)   // 项目编号
  def vs = column[String]("version", O.NotNull) // 当前版本
  def updated= column[DateTime]("updated", O.Default(DateTime.now()))

  override def * = (id.?, projectId, vs, updated) <> (Version.tupled, Version.unapply _)
  def idx = index("idx_pid", (projectId, updated))
  def idx_vs = index("idx_pid_vs", (projectId, vs), unique = true)
}
object VersionHelper extends PlayCache {

  import models.AppDB._

  val qVersion = TableQuery[VersionTable]
  val qConf = TableQuery[ConfTable]

  def findById(id: Int) = db withSession { implicit session =>
    qVersion.filter(_.id === id).firstOption
  }

  def findByProjectId(projectId: Int): Seq[Version] = db withSession { implicit session =>
    qVersion.filter(_.projectId === projectId).sortBy(_.updated desc).list
  }

  def findByProjectId_Vs(projectId: Int, vs: String): Option[Version] = db withSession { implicit session =>
    qVersion.filter(v => v.projectId === projectId && v.vs === vs).firstOption
  }

  def findByProjectId_EnvId(projectId: Int, envId: Int): Seq[Version] = db withSession { implicit session =>
    EnvironmentHelper.findById(envId) match {
      case Some(env) =>
        val list = findByProjectId(projectId)
        env.level match {
          case LevelEnum.unsafe => list
          case _ => list.filter(t => t.vs.endsWith("RELEASE"))
        }
      case None => Nil
    }
  }

  def count(projectId: Int) = db withSession { implicit session =>
    qVersion.filter(_.projectId === projectId).length.run
  }

  def all(projectId: Int, page: Int, pageSize: Int): Seq[Version] = db withSession { implicit session =>
    val offset = pageSize * page
    qVersion.filter(_.projectId === projectId).sortBy(_.updated desc).drop(offset).take(pageSize).list
  }

  def all(projectId: Int, top: Int): Seq[Version] = db withSession { implicit session =>
    qVersion.filter(_.projectId === projectId).sortBy(_.updated desc).take(top).list
  }

  @throws[UniqueNameException]
  def create(version: Version) = db withTransaction { implicit session =>
    try {
      val versionId = qVersion.returning(qVersion.map(_.id)).insert(version)
      ProjectHelper.findById(version.projectId) match {
          case Some(p) =>
            ProjectHelper._update(version.projectId, Project(p.id, p.name, p.description, p.templateId, p.subTotal + 1, Some(versionId), Some(version.vs), Some(version.updated)))
          case None =>
      }
      //增加配置文件copy
//      _copyConfigs(version.copy(Option(versionId)), false)(session)
      versionId
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def copyConfigs(envId: Int, projectId: Int, versionId: Int)= db withTransaction { implicit session =>
    val defConf = qConf.filter(c => c.envId === envId && c.projectId === projectId && c.versionId === 0).firstOption

    val conf = if (defConf.isDefined) { defConf } else {
      VersionHelper.findById(versionId) match {
        case Some(v) => qConf.filter(c =>
          c.envId === envId && c.projectId === projectId && c.updated <= v.updated).sortBy(_.updated desc).firstOption
        case _ => None
      }
    }

    conf match {
      case Some(c) =>
        ConfHelper.findByEnvId_ProjectId_VersionId(c.envId, projectId, c.versionId) foreach { c =>
          val content = ConfContentHelper.findById(c.id.get)
          ConfHelper._create(c.copy(id = None, envId = envId, versionId = versionId), content)
        }
      case None =>
    }

  }

  def delete(version: Version, cids: Seq[Int]): Int = db withTransaction { implicit session =>
    ProjectHelper.findById(version.projectId) match {
      case Some(p) =>
        val total = if (p.subTotal - 1 < 0) 0 else p.subTotal - 1 // prevent -1
        ProjectHelper._update(version.projectId, p.copy(subTotal = total))
      case None =>
    }
    cids foreach { cid =>
      ConfHelper._delete(cid)
      ConfLogHelper._deleteByConfId(cid)
    }
    qVersion.filter(_.id === version.id).delete
  }

  @throws[UniqueNameException]
  def update(id: Int, version: Version) = db withSession { implicit session =>
    try {
      val version2update = version.copy(Option(id))
      qVersion.filter(_.id === id).update(version2update)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

}