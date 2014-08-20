package models.conf

import models.PlayCache
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目环境变量
 */

case class Variable(id: Option[Int], envId: Int, projectId: Option[Int], name: String, value: String)

class VariableTable(tag: Tag) extends Table[Variable](tag, "variable") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def projectId = column[Int]("project_id")
  def name = column[String]("name", O.DBType("varchar(254)"))
  def value = column[String]("value")

  override def * = (id.?, envId, projectId.?, name, value) <> (Variable.tupled, Variable.unapply _)

  def idx = index("idx_name", (envId, projectId, name), unique = true)
}

object VariableHelper extends PlayCache {

  import models.AppDB._

  val qVariable = TableQuery[VariableTable]

  def findById(id: Int): Option[Variable] = db withSession { implicit session =>
    qVariable.filter(_.id === id).firstOption
  }

  def findByEnvId(envId: Int): Seq[Variable] = db withSession { implicit session =>
    qVariable.filter(_.envId === envId).list
  }

  def findByProjectId(projectId: Int): Seq[Variable] = db withSession { implicit session =>
    qVariable.filter(_.projectId === projectId).list
  }

  def findByEnvId_ProjectId(envId: Int, projectId: Int): Seq[Variable] = db withSession { implicit session =>
    qVariable.filter(v => v.envId === envId && v.projectId === projectId).list
  }

  def _create(variables: Seq[Variable])(implicit session: JdbcBackend#Session) = {
    qVariable.insertAll(variables: _*)(session)
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qVariable.filter(_.projectId === projectId).delete(session)
  }

  def _deleteByEnvId_ProjectId(envId: Int, projectId: Int)(implicit session: JdbcBackend#Session) = {
    qVariable.filter(v => v.envId ===envId && v.projectId === projectId).delete(session)
  }

}
