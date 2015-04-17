package models.conf


import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by jinwei on 9/8/14.
 */
case class ProjectDependency(id: Option[Int], projectId: Int, dependencyId: Int, alias: Option[String])

class ProjectDependencyTable(tag: Tag) extends Table[ProjectDependency](tag, "project_dependency"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id", O.NotNull)
  def dependencyId = column[Int]("dependency_id", O.NotNull)
  def alias = column[Option[String]]("alias", O.Nullable)
  override def * = (id.?, projectId, dependencyId, alias) <> (ProjectDependency.tupled, ProjectDependency.unapply _)

  def idx_path = index("idx_pDependency", (projectId, dependencyId), unique = true)
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

  def add(depend: ProjectDependency) = db withSession { implicit session =>
    qpd.insert(depend)
  }

  def remove(depend: ProjectDependency) = db withSession { implicit session =>
    qpd.filter(t => t.projectId === depend.projectId && t.dependencyId === depend.dependencyId).delete
  }

  def update(depend: ProjectDependency, newId: Int) = db withSession { implicit session =>
    qpd.filter(t => t.projectId === depend.projectId && t.dependencyId === depend.dependencyId).map(_.dependencyId).update(newId)
  }

  def updateAlias(pd: ProjectDependency) = db withSession {implicit session =>
    qpd.filter(t => t.projectId === pd.projectId && t.dependencyId === pd.dependencyId).map(_.alias).update(pd.alias)
  }
}