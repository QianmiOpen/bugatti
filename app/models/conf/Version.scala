package models.conf

import enums.LevelEnum
import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime
import utils.TaskTools

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

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

  val SNAPSHOTSUFFIX = "-SNAPSHOT"

  val qVersion = TableQuery[VersionTable]

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
          case _ => list.filterNot(t => t.vs.endsWith(SNAPSHOTSUFFIX))
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

  def create(version: Version) = db withTransaction { implicit session =>
    val versionId = qVersion.returning(qVersion.map(_.id)).insert(version)
    ProjectHelper.findById(version.projectId) match {
        case Some(p) =>
          ProjectHelper._update(version.projectId, Project(p.id, p.name, p.templateId, p.subTotal + 1, Some(versionId), Some(version.vs), Some(version.updated)))
        case None =>
    }
    versionId
  }

  def delete(version: Version): Int = db withTransaction { implicit session =>
    ProjectHelper.findById(version.projectId) match {
      case Some(p) =>
        val total = if (p.subTotal - 1 < 0) 0 else p.subTotal - 1 // prevent -1
        ProjectHelper._update(version.projectId, p.copy(subTotal = total))
      case None =>
    }
    qVersion.filter(_.id is version.id).delete
  }

  def update(id: Int, version: Version) = db withSession { implicit session =>
    val version2update = version.copy(Option(id))
    qVersion.filter(_.id === id).update(version2update)
  }

}