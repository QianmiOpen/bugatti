package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * 环境和项目的关系配置
 */
case class EnvironmentProjectRel(id: Option[Int], envId: Option[Int], projectId: Option[Int], syndicName: String, name: String, ip: String)
case class EnvironmentProjectRelForm(id: Option[Int], envId: Int, projectId: Int, ips: Seq[IP]) {
  def toRelations = for (ip <- ips; rel = EnvironmentProjectRel(id, Some(envId), Some(projectId),"", ip.name, ip.ip)) yield rel
}
case class IP(ip: String, name: String)

class EnvironmentProjectRelTable(tag: Tag) extends Table[EnvironmentProjectRel](tag, "environment_project_rel") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.Nullable)
  def projectId = column[Int]("project_id", O.Nullable)
  def syndicName = column[String]("syndic_name", O.NotNull)
  def name = column[String]("name", O.Nullable)
  def ip = column[String]("ip", O.Nullable)

  override def * = (id.?, envId.?, projectId.?, syndicName, name, ip) <> (EnvironmentProjectRel.tupled, EnvironmentProjectRel.unapply _)
}

object EnvironmentProjectRelHelper {
  import models.AppDB._
  val qRelation = TableQuery[EnvironmentProjectRelTable]
  val qEnv = TableQuery[EnvironmentTable]
  val qProject = TableQuery[ProjectTable]

  def add(relationForm: EnvironmentProjectRelForm) = db withTransaction { implicit session =>
    qRelation.insertAll(relationForm.toRelations: _*)
  }

  def create(envProjectRel: EnvironmentProjectRel) = db withSession { implicit session =>
    qRelation.returning(qRelation.map(_.id)).insert(envProjectRel)
  }

  def findByEnvId_ProjectId(envId: Int, projectId: Int): Seq[EnvironmentProjectRel] = db withSession {
    implicit session =>
      qRelation.where(r => r.envId === envId && r.projectId === projectId).list
  }

  def findByIp(ip: String): Seq[EnvironmentProjectRel] = db withSession {
    implicit session =>
      qRelation.where(_.ip === ip).list
  }

  def findBySyndicName(syndicName: String): Seq[EnvironmentProjectRel] = db withSession{ implicit session =>
    qRelation.where(_.syndicName === syndicName).list
  }

  def findById(id: Int): Option[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.where(_.id === id).firstOption
  }

  def findIpsByEnvId(envId: Int): Seq[String] = db withSession { implicit session =>
    qRelation.where(_.envId === envId).list.map(_.ip)
  }

  def all(envId: Option[Int], projectId: Option[Int], page: Int, pageSize: Int): Seq[(EnvironmentProjectRel, String, String)] = db withSession { implicit session =>
    val offset = pageSize * page
    var query = for {
      r <- qRelation
      e <- qEnv if r.envId === e.id
      p <- qProject if r.projectId === p.id
    } yield (r, e.name, p.name)
    envId.map(id => query = query.filter(_._1.envId === id))
    projectId.map(id => query = query.filter(_._1.projectId === id))
    query.drop(offset).take(pageSize).list
  }

  def count(envId: Option[Int], projectId: Option[Int]): Int = db withSession { implicit session =>
    var query = for {
      r <- qRelation
      e <- qEnv if r.envId === e.id
      p <- qProject if r.projectId === p.id
    } yield r
    envId.map(id => query = query.filter(_.envId === id))
    projectId.map(id => query = query.filter(_.projectId === id))
    Query(query.length).first
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    qRelation.where(_.id === id).delete
  }

  def deleteAll(ids: Array[String]) = db withSession { implicit session =>
    for {
      id <- ids
      if qRelation.where(_.id === id.toInt).delete == 0
    } yield id
  }

  def findEmptyEnvsBySyndicName(syndicName: String): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.where(c => c.syndicName === syndicName && c.envId.isNull).list
  }

  def update(envProjectRel: EnvironmentProjectRel) = db withSession { implicit session =>
    qRelation.where(_.id === envProjectRel.id).update(envProjectRel)
  }

}