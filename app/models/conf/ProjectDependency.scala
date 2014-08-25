package models.conf

import controllers.conf.DependencyNest

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 9/8/14.
 */
case class ProjectDependency(id: Option[Int], projectId: Int, dependencyId: Int)

class ProjectDependencyTable(tag: Tag) extends Table[ProjectDependency](tag, "project_dependency"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id", O.NotNull)
  def dependencyId = column[Int]("dependency_id", O.NotNull)
  override def * = (id.?, projectId, dependencyId) <> (ProjectDependency.tupled, ProjectDependency.unapply _)

  def idx_path = index("idx_relation", (projectId, dependencyId), unique = true)
}

object ProjectDependencyHelper {
  import models.AppDB._
  val qpd = TableQuery[ProjectDependencyTable]

  def all: Seq[ProjectDependency] = db withSession{ implicit session =>
    qpd.list
  }

  def findByProjectId(pId: Int): Seq[ProjectDependency] = db withSession {implicit session =>
    qpd.filter(_.projectId === pId).list
  }

  def deleteByProjectId(pid: Int) = db withSession { implicit session =>
    qpd.filter(t => t.projectId === pid || t.dependencyId === pid).delete
  }

  def insertWithSeq(seq: Seq[ProjectDependency]) = db withSession {implicit session =>
    qpd.insertAll(seq: _*)
  }

  def add(pd: ProjectDependency) = db withSession { implicit session =>
    qpd.insert(pd)
  }

  def removeByP_C(parentId: Int, childId: Int) = db withSession { implicit session =>
    qpd.filter(t => t.projectId === parentId && t.dependencyId === childId).delete
  }

  def addByP_C(parent: DependencyNest, child: Project) = db withSession { implicit session =>
    val result = qpd.insert(ProjectDependency(None, parent.id, child.id.get))
    result
  }
}