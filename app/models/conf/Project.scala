package models.conf

import enums.LevelEnum
import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

import scala.slick.jdbc.JdbcBackend

/**
 * 项目
 * 项目为综合型，通过类型标示不同
 *
 * @author of546
 */
case class Project(id: Option[Int], name: String, templateId: Int, subTotal: Int, lastVersion: Option[String], lastUpdated: Option[DateTime])
class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def templateId = column[Int]("template_id", O.NotNull)  // 项目模板编号
  def subTotal = column[Int]("sub_total", O.NotNull, O.Default(0)) // 子项目数量
  def lastVersion = column[String]("last_version", O.Nullable)
  def lastUpdated= column[DateTime]("last_updated", O.Nullable, O.Default(DateTime.now()))

  override def * = (id.?, name, templateId, subTotal, lastVersion.?, lastUpdated.?) <> (Project.tupled, Project.unapply _)
  def idx = index("idx_name", name, unique = true)
  def idx_template = index("idx_template", templateId)
}

object ProjectHelper extends PlayCache {

  import models.AppDB._

  val qProject = TableQuery[ProjectTable]

  def findById(id: Int) = db withSession { implicit session =>
    qProject.where(_.id is id).firstOption
  }

  def findByName(name: String) = db withSession { implicit session =>
    qProject.where(_.name is name).firstOption
  }

  def count: Int = db withSession { implicit session =>
    Query(qProject.length).first
  }

  def all(page: Int, pageSize: Int): List[Project] = db withSession { implicit session =>
    val offset = pageSize * page
    qProject.drop(offset).take(pageSize).list
  }

  def create(project: Project) = db withSession { implicit session =>
    qProject.insert(project)
  }

  def create_(project: Project)(implicit session: JdbcBackend#Session) = {
    qProject.returning(qProject.map(_.id)).insert(project)(session)
  }

  def create(project: Project, jobNo: String) = db withTransaction { implicit session =>
    val pid = create_(project)
    val member = Member(None, pid, LevelEnum.safe, jobNo)
    MemberHelper.create_(member)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qProject.where(_.id is id).delete
  }

  def update(id: Int, project: Project) = db withSession { implicit session =>
    val project2update = project.copy(Some(id))
    qProject.where(_.id is id).update(project2update)
  }

}
