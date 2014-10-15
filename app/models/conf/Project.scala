package models.conf

import actor.ActorUtils
import actor.conf.UpdateProject
import com.github.tototoshi.slick.MySQLJodaSupport._
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import enums.LevelEnum
import exceptions.UniqueNameException
import models.{MaybeFilter, PlayCache}
import org.joda.time.DateTime
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

case class Project(id: Option[Int], name: String, description: Option[String], templateId: Int, subTotal: Int, lastVid: Option[Int], lastVersion: Option[String], lastUpdated: Option[DateTime])
case class ProjectForm(id: Option[Int], name: String, description: Option[String], templateId: Int, subTotal: Int, lastVid: Option[Int], lastVersion: Option[String], lastUpdated: Option[DateTime], items: Seq[Attribute], variables: Seq[Variable]) {
  def toProject = Project(id, name, description, templateId, subTotal, lastVid, lastVersion, lastUpdated)
}

class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description", O.Nullable, O.DBType("VARCHAR(500)"))
  def templateId = column[Int]("template_id")           // 项目模板编号
  def subTotal = column[Int]("sub_total", O.Default(0)) // 版本数量
  def lastVid = column[Int]("last_version_id", O.Nullable)         // 最近版本id
  def lastVersion = column[String]("last_version", O.Nullable)     // 最近版本号
  def lastUpdated= column[DateTime]("last_updated", O.Nullable, O.Default(DateTime.now()))

  override def * = (id.?, name, description.?, templateId, subTotal, lastVid.?, lastVersion.?, lastUpdated.?) <> (Project.tupled, Project.unapply _)
  def idx = index("idx_name", name, unique = true)
  def idx_template = index("idx_template", templateId)

}

object ProjectHelper extends PlayCache {

  import models.AppDB._

  val ProjectNotExistId = -1

  val qProject = TableQuery[ProjectTable]
  val qMember = TableQuery[ProjectMemberTable]
  val qpd = TableQuery[ProjectDependencyTable]

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

  def allExceptSelf(id: Int): Seq[Project] = db withSession { implicit session =>
    qProject.filterNot(_.id === id).list
  }

  def allByTemplateId(templateId: Int): Seq[Project] = db withSession { implicit session =>
    qProject.filter(_.templateId === templateId).list
  }

  def create(project: Project) = db withSession { implicit session =>
    _create(project)
  }

  def create(projectForm: ProjectForm, jobNo: String) = db withTransaction { implicit session =>
    val _projectId = _create(projectForm.toProject)
    // variable
    val variables = projectForm.variables.map(vb => vb.copy(None, projectId = Some(_projectId)))
    VariableHelper._create(variables)
    // attribute
    val attrs = projectForm.items.map(item => item.copy(None, Some(_projectId)))
    AttributeHelper._create(attrs)
    // member
    ProjectMemberHelper._create(ProjectMember(None, _projectId, LevelEnum.safe, jobNo))
    _projectId

  }

  @throws[UniqueNameException]
  def _create(project: Project)(implicit session: JdbcBackend#Session) = {
    try {
      val pid = qProject.returning(qProject.map(_.id)).insert(project)(session)
      //增加项目依赖初始关系
      TemplateHelper.findById(project.templateId) match {
        case Some(template) =>
          ProjectDependencyHelper.insertWithSeq(template.dependentProjectIds.map(x => ProjectDependency(None, pid, x)))
        case None => // ignore
      }
      //修改缓存
      ActorUtils.configuarActor ! UpdateProject(pid, project.name)
      pid
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def delete(id: Int) = db withTransaction { implicit session =>
    // relation
    EnvironmentProjectRelHelper._unbindByProjectId(Some(id))
    // attribute
    AttributeHelper._deleteByProjectId(id)
    // member
    ProjectMemberHelper._deleteByProjectId(id)
    // variable
    VariableHelper._deleteByProjectId(id)
    val result = qProject.filter(_.id === id).delete
    //删除项目依赖初始关系
    ProjectDependencyHelper.deleteByProjectId(id)
    result
  }

  def update(id: Int, envId: Int, projectForm: ProjectForm) = db withTransaction { implicit session =>
    // attribute
    val attrs = AttributeHelper.findByProjectId(id)
    projectForm.items.filterNot(pa => attrs.exists( _a => _a.projectId == Some(id) && _a.name == pa.name)).foreach { attr =>
      AttributeHelper._create(attr.copy(None, Some(id)))
    }

    projectForm.items.filter(pa => attrs.exists(_a => _a.projectId == Some(id) && _a.name == pa.name)).foreach { attr =>
      AttributeHelper._update(attr.copy(projectId = Some(id))) // id
    }

    // variable
    VariableHelper._deleteByEnvId_ProjectId(envId, id)
    val variables = projectForm.variables.filter(_.envId == Some(envId)).map(vb => vb.copy(None, projectId = Some(id)))
    VariableHelper._create(variables)

    // project
    _update(id, projectForm.toProject)
  }

  @throws[UniqueNameException]
  def _update(id: Int, project: Project)(implicit session: JdbcBackend#Session) = {
    try {
      val project2update = project.copy(Some(id))
      val result = qProject.filter(_.id === id).update(project2update)(session)
      //修改缓存
      ActorUtils.configuarActor ! UpdateProject(id, project.name)
      result
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

//  ====================================================

  def findDependencyProjects(id: Int): Seq[Project]= db withSession { implicit session =>
    (for{
      (p, pd) <- qProject innerJoin qpd on(_.id === _.dependencyId) if pd.projectId === id
    } yield p).list
  }

}
