package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import enums.LevelEnum
import exceptions.UniqueNameException
import play.api.Logger
import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime
import utils.TaskTools

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

import scala.slick.jdbc.JdbcBackend

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
      _copyConfigs(version.copy(Option(versionId)), false)(session)
      versionId
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def _copyConfigs(version: Version, safeExcept: Boolean)(implicit session: JdbcBackend#Session) = {
    //1、findAllEnvs except safe
    val envs = EnvironmentHelper.findByUnsafe()
    Logger.info(s"envs => ${envs}")
    //2、envs foreach
    envs.foreach {
      e =>
        //找到当前环境 或者 模板 中最近的版本配置
        (for{
          (conf, ver) <- qConf innerJoin qVersion on(_.versionId === _.id)
          if ((conf.envId === e.id.get || conf.envId === 0) && conf.projectId === version.projectId && ver.updated < version.updated)
        } yield (conf.envId, ver)).sortBy(t => t._2.updated desc).firstOption match {
          case Some(v) =>
            Logger.info(s"v => ${v._1}, ${v._2}")
            val confs = ConfHelper.findByEnvId_VersionId(v._1, v._2.id.get)
            Logger.info(s"confs => ${e.id} =>${confs}")
              confs.foreach { c =>
              Logger.info(s"c => ${c}")
              val content = ConfContentHelper.findById(c.id.get)
              ConfHelper._create(c.copy(id = None, envId = e.id.get, versionId = version.id.get), content)(session)
            }
          case _ =>
            Logger.warn(s"没有找到合适的配置文件!")
        }
    }
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