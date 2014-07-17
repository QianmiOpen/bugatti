package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * 环境和项目的关系配置
 */
case class EnvironmentProjectRel(id: Option[Int], envId: Option[Int], projectId: Option[Int], syndicName: String, name: String, ip: String)
case class EnvRelForm(envId: Int, projectId: Int, ids: Seq[Int])

class EnvironmentProjectRelTable(tag: Tag) extends Table[EnvironmentProjectRel](tag, "environment_project_rel") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.Nullable)
  def projectId = column[Int]("project_id", O.Nullable)
  def syndicName = column[String]("syndic_name", O.NotNull)
  def name = column[String]("name", O.NotNull)
  def ip = column[String]("ip", O.NotNull)

  override def * = (id.?, envId.?, projectId.?, syndicName, name, ip) <> (EnvironmentProjectRel.tupled, EnvironmentProjectRel.unapply _)
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

  def all(envId: Option[Int], projectId: Option[Int], page: Int, pageSize: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    val offset = pageSize * page
    var query = for { r <- qRelation } yield r
    envId.map(id => query = query.filter(_.envId === id))
    projectId.map(id => query = query.filter(_.projectId === id))
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

  def updateByProjectId(relForm: EnvRelForm): Int = db withSession { implicit session =>
    relForm.ids.map { id =>
      val q = for { r <- qRelation if r.id === id } yield r.projectId
      q.update(relForm.projectId)
    }.size
  }

  def unbind(rel: EnvironmentProjectRel) = db withTransaction { implicit session =>
    qRelation.filter(_.id === rel.id).update(rel.copy(projectId = None))
  }

  def update(envProjectRel: EnvironmentProjectRel) = db withSession { implicit session =>
    qRelation.filter(_.id === envProjectRel.id).update(envProjectRel)
  }

}