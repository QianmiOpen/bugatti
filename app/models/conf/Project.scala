package models.conf

import com.github.tototoshi.slick.MySQLJodaSupport._
import enums.LevelEnum
import models.{MaybeFilter, PlayCache}
import org.joda.time.DateTime
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

case class Project(id: Option[Int], name: String, templateId: Int, subTotal: Int, lastVid: Option[Int], lastVersion: Option[String], lastUpdated: Option[DateTime], globalVariable: Seq[Variable])
case class ProjectForm(id: Option[Int], name: String, templateId: Int, subTotal: Int, lastVid: Option[Int], lastVersion: Option[String], lastUpdated: Option[DateTime], globalVariable: Seq[Variable], items: Seq[Attribute]) {
  def toProject = Project(id, name, templateId, subTotal, lastVid, lastVersion, lastUpdated, globalVariable)
}

class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def templateId = column[Int]("template_id")           // 项目模板编号
  def subTotal = column[Int]("sub_total", O.Default(0)) // 版本数量
  def lastVid = column[Int]("last_version_id", O.Nullable)         // 最近版本id
  def lastVersion = column[String]("last_version", O.Nullable)     // 最近版本号
  def lastUpdated= column[DateTime]("last_updated", O.Nullable, O.Default(DateTime.now()))
  def globalVariable = column[Seq[Variable]]("global_variable", O.DBType("text"))(MappedColumnType.base[Seq[Variable], String](
    _.map(v => s"${v.name}:${v.value}").mkString(","),
    _.split(",").filterNot(_.trim.isEmpty).map(_.split(":") match { case Array(name, value) => Variable(name, value) }).toList
  ))

  override def * = (id.?, name, templateId, subTotal, lastVid.?, lastVersion.?, lastUpdated.?, globalVariable) <> (Project.tupled, Project.unapply _)
  def idx = index("idx_name", name, unique = true)
  def idx_template = index("idx_template", templateId)

}

object ProjectHelper extends PlayCache {

  import models.AppDB._

  val qProject = TableQuery[ProjectTable]
  val qMember = TableQuery[MemberTable]

  def findById(id: Int): Option[Project] = db withSession { implicit session =>
    qProject.filter(_.id === id).firstOption
  }

  def findByName(name: String): Option[Project] = db withSession { implicit session =>
    qProject.filter(_.name === name).firstOption
  }

  def countByTemplateId(templateId: Int) = db withSession { implicit session =>
    qProject.filter(_.templateId === templateId).length.run
  }

  def count(projectName: Option[String], jobNo: Option[String]): Int = db withSession { implicit session =>
    jobNo match {
      case Some(no) =>
        val queryJoin = (for {
          p <- qProject
          m <- qMember if p.id === m.projectId
        } yield (p, m)).filter(_._2.jobNo === jobNo)
        val query = MaybeFilter(queryJoin.map(_._1)).filter(projectName)(v => b => b.name like s"${v}%").query
        query.length.run
      case None =>
        MaybeFilter(qProject).filter(projectName)(v => b => b.name like s"${v}%").query.length.run
    }
  }

  def all(projectName: Option[String], jobNo: Option[String], page: Int, pageSize: Int): Seq[Project] = db withSession { implicit session =>
    val offset = pageSize * page
    jobNo match {
      case Some(no) =>
        val queryJoin = (for {
          p <- qProject
          m <- qMember if p.id === m.projectId
        } yield (p, m)).filter(_._2.jobNo === jobNo)
        val query = MaybeFilter(queryJoin.map(_._1)).filter(projectName)(v => b => b.name like s"${v}%").query
        query.drop(offset).take(pageSize).list
      case None =>
        val query = MaybeFilter(qProject).filter(projectName)(v => b => b.name like s"${v}%").query
        query.drop(offset).take(pageSize).list
    }

  }

  def all(): Seq[Project] = db withSession { implicit session =>
    qProject.list
  }

  def create(project: Project) = db withSession { implicit session =>
    _create(project)
  }

  def create(projectForm: ProjectForm, jobNo: String) = db withTransaction { implicit session =>
    val pid = _create(projectForm.toProject)
    val attrs = projectForm.items.map(item => item.copy(None, Some(pid)))
    AttributeHelper._create(attrs)
    MemberHelper._create(Member(None, pid, LevelEnum.safe, jobNo))
    pid
  }

  def _create(project: Project)(implicit session: JdbcBackend#Session) = {
    qProject.returning(qProject.map(_.id)).insert(project)(session)
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    EnvironmentProjectRelHelper._unbindByProjectId(Some(id))
    AttributeHelper._deleteByProjectId(id)
    MemberHelper._deleteByProjectId(id)
    qProject.filter(_.id === id).delete
  }

  def update(id: Int, projectForm: ProjectForm) = db withSession { implicit session =>
    AttributeHelper._deleteByProjectId(id)
    val attrs = projectForm.items.map(item => item.copy(None, Some(id)))
    AttributeHelper._create(attrs)
    _update(id, projectForm.toProject)
  }

  def _update(id: Int, project: Project)(implicit session: JdbcBackend#Session) = {
    val project2update = project.copy(Some(id))
    qProject.filter(_.id === id).update(project2update)(session)
  }

}
