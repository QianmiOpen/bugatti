package models.conf

import models.MaybeFilter

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current
import enums.ContainerTypeEnum
import enums.ContainerTypeEnum.Container
import scala.slick.jdbc.JdbcBackend

/**
 * 环境和项目的关系配置
 */
case class EnvironmentProjectRel(id: Option[Int], envId: Option[Int], projectId: Option[Int], areaId: Option[Int],
                                 syndicName: String, name: String, ip: String,
                                 containerType: Container, hostIp: Option[String], hostName: Option[String],
                                 globalVariable: Seq[Variable])
case class EnvRelForm(envId: Int, projectId: Int, ids: Seq[Int])

class EnvironmentProjectRelTable(tag: Tag) extends Table[EnvironmentProjectRel](tag, "environment_project_rel") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id", O.Nullable)
  def projectId = column[Int]("project_id", O.Nullable)
  def areaId = column[Int]("area_id", O.Nullable)
  def syndicName = column[String]("syndic_name")
  def name = column[String]("name")
  def ip = column[String]("ip")
  def containerType = column[Container]("container_type", O.Default(ContainerTypeEnum.vm), O.DBType("ENUM('vm', 'docker')"))
  def hostIp = column[String]("host_ip", O.Nullable)
  def hostName = column[String]("host_name", O.Nullable)
  def globalVariable = column[Seq[Variable]]("global_variable", O.DBType("text"))(MappedColumnType.base[Seq[Variable], String](
    _.filter(!_.value.isEmpty).map(v => s"${v.name}:${v.value}").mkString(","),
    _.split(",").filterNot(_.trim.isEmpty).map(_.split(":") match { case Array(name, value) => new Variable(name, value) }).toList
  ))

  override def * = (id.?, envId.?, projectId.?, areaId.?, syndicName, name, ip, containerType, hostIp.?, hostName.?, globalVariable) <> (EnvironmentProjectRel.tupled, EnvironmentProjectRel.unapply _)
  index("idx_eid_pid", (envId, projectId))
  index("idx_ip", ip, unique = true)
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

  def findByEnvId_AreaId(envId: Int, areaId: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(r => r.envId === envId && r.areaId === areaId).list
  }

  def findUnbindByEnvId_AreaId(envId: Int, areaId: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    qRelation.filter(r => r.envId === envId && r.areaId === areaId && r.projectId.isNull).list
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

  def all(ip: Option[String], envId: Option[Int], projectId: Option[Int], sort: Option[String], direction: Option[String], page: Int, pageSize: Int): Seq[EnvironmentProjectRel] = db withSession { implicit session =>
    val offset = pageSize * page
    var query = MaybeFilter(qRelation)
      .filter(envId)(v => b => b.envId === v)
      .filter(projectId)(v => b => b.projectId === projectId)
      .filter(ip)(v => b => b.ip === ip).query
    sort match {
      case Some(s) if s == "ip" =>
        query = direction match { case Some(d) if d == "desc" => query.sortBy(_.ip desc); case _ => query.sortBy(_.ip asc)}
      case _ =>
        query = query.sortBy(_.projectId desc) // default sort by projectId
    }
    query.drop(offset).take(pageSize).list
  }

  def count(ip: Option[String], envId: Option[Int], projectId: Option[Int]): Int = db withSession { implicit session =>
    val query = MaybeFilter(qRelation)
      .filter(envId)(v => b => b.envId === v)
      .filter(projectId)(v => b => b.projectId === projectId)
      .filter(ip)(v => b => b.ip === ip).query
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