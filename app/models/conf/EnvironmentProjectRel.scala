package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

import scala.slick.jdbc.JdbcBackend

/**
 * 环境和项目的关系配置
 */
case class EnvironmentProjectRel(id: Option[Int], envId: Option[Int], projectId: Option[Int], syndicName: String, name: String, ip: String)
case class EnvRelForm(envId: Int, projectId: Int, ids: Seq[Int])

class EnvironmentProjectRelTable(tag: Tag) extends Table[EnvironmentProjectRel](tag, "environment_project_rel") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.Nullable)
  def projectId = column[Int]("project_id", O.Nullable)
  def syndicName = column[String]("syndic_name")
  def name = column[String]("name")
  def ip = column[String]("ip")

  override def * = (id.?, envId.?, projectId.?, syndicName, name, ip) <> (EnvironmentProjectRel.tupled, EnvironmentProjectRel.unapply _)
  index("idx_eid_pid", (envId, projectId))
  index("idx_ip", ip)
}

object EnvironmentProjectRelHelper {
  import models.AppDB._
  val qRelation = TableQuery[EnvironmentProjectRelTable]
  val qEnv = TableQuery[EnvironmentTable]
  val qProject = TableQuery[ProjectTable]

  def findById(id: Int): Option[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(_.id === id).firstOption
  }

  def findByIp(ip: String): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(_.ip === ip).list
  }

  def findBySyndicName(syndicName: String): Seq[EnvironmentProjectRel] = db withSession{ implicit session =>
    qRelation.filter(_.syndicName === syndicName).list
  }

  def findByEnvId_ProjectId(envId: Int, projectId: Int): Seq[EnvironmentProjectRel] = db withSession {
    implicit session =>
      qRelation.filter(r => r.envId === envId && r.projectId === projectId).list
  }

  def findEmptyEnvsBySyndicName(syndicName: String): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(c => c.syndicName === syndicName && c.envId.isNull).list
  }

  def findIpsByEnvId(envId: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(r => r.envId === envId && r.projectId.isNull).list
  }

  def allNotEmpty: Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(r => r.envId.isNotNull && r.projectId.isNotNull).list
  }

  def all(envId: Option[Int], projectId: Option[Int], sort: Option[String], direction: Option[String], page: Int, pageSize: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    val offset = pageSize * page
    var query = for { r <- qRelation } yield r
    envId.map(id => query = query.filter(_.envId === id))
    projectId.map(id => query = query.filter(_.projectId === id))
    sort match {
      case Some(s) if s == "ip" =>
        query = direction match { case Some(d) if d == "desc" => query.sortBy(_.ip desc); case _ => query.sortBy(_.ip asc)}
      case _ =>
        query = query.sortBy(_.projectId desc) // default sort by projectId
    }
    query.drop(offset).take(pageSize).list
  }

  def count(envId: Option[Int], projectId: Option[Int]): Int = db withSession { implicit session =>
    var query = for { r <- qRelation } yield r
    envId.map(id => query = query.filter(_.envId === id))
    projectId.map(id => query = query.filter(_.projectId === id))
    query.length.run
  }
  
  def create(envProjectRel: EnvironmentProjectRel) = db withSession { implicit session =>
    qRelation.returning(qRelation.map(_.id)).insert(envProjectRel)
  }

  def bind(relForm: EnvRelForm): Int = db withSession { implicit session =>
    relForm.ids.map { id =>
      qRelation.filter(_.id === id).map(_.projectId).update(relForm.projectId)
    }.size
  }

  def _unbindByProjectId(projectId: Option[Int])(implicit session: JdbcBackend#Session) = {
    qRelation.filter(_.projectId === projectId).map(_.projectId.?).update(None)(session)
  }

  def unbind(rel: EnvironmentProjectRel) = db withTransaction { implicit session =>
    qRelation.filter(_.id === rel.id).update(rel.copy(projectId = None))
  }

  def update(rel: EnvironmentProjectRel) = db withSession { implicit session =>
    qRelation.filter(_.id === rel.id).update(rel)
  }

}