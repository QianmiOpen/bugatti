package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._

/**
 * 项目类型
 *
 * @author of546
 */
case class ProjectType(id: Int, name: String)
class ProjectTypeTable(tag: Tag) extends Table[ProjectType](tag, "project_type") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)

  override def *  = (id, name) <> (ProjectType.tupled, ProjectType.unapply _)
  def idx = index("idx_name", name, unique = true)
}
object ProjectTypeHelper extends PlayCache {

  import models.AppDB._

  val qProjectType = TableQuery[ProjectTypeTable]

  def all = db withSession { implicit session =>
    qProjectType.list
  }

  def findById(id: Int) = db withSession { implicit session =>
    qProjectType.where(_.id is id).firstOption
  }

  def findByName(name: String) = db withSession { implicit session =>
    qProjectType.where(_.name is name).firstOption
  }

  def create(projectType: ProjectType) = db withSession { implicit session =>
    qProjectType.insert(projectType)
  }

  def update(id: Int, projectType: ProjectType) = db withSession { implicit session =>
    val ptype2update = projectType.copy(id)
    qProjectType.where(_.id is id).update(ptype2update)
  }

}
