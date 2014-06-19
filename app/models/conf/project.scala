package models.conf

import play.api.Play.current
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * 项目
 */
case class Project(id: Option[Int], name: String, typeId: Int, lastVersion: Option[String], lastUpdated: Option[DateTime])
class ProjectTable(tag: Tag) extends Table[Project](tag, "project") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def typeId = column[Int]("type_id", O.NotNull)
  def lastVersion = column[String]("last_version")
  def lastUpdated= column[DateTime]("last_updated", O.Default(DateTime.now()))

  override def * = (id.?, name, typeId, lastVersion.?, lastUpdated.?) <> (Project.tupled, Project.unapply _)
  def idx = index("idx_name", name, unique = true)
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

  def create(project: Project) = db withSession { implicit session =>
    qProject.insert(project)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qProject.where(_.id is id).delete
  }

  def update(id: Int, project: Project) = db withSession { implicit session =>
    val project2update = project.copy(Some(id))
    qProject.where(_.id is id).update(project2update)
  }

}
