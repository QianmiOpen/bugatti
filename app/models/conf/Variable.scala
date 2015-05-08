package models.conf

import models.PlayCache
import play.api.Play.current
import enums.LevelEnum
import enums.LevelEnum.Level
import utils.SecurityUtil

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目环境变量
 */

case class Variable(id: Option[Int], envId: Option[Int], projectId: Option[Int], name: String, value: String, level: Level) {
  def this(name: String, value: String) = this(None, None, None, name, value, LevelEnum.unsafe)
}

class VariableTable(tag: Tag) extends Table[Variable](tag, "variable") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def projectId = column[Int]("project_id")
  def name = column[String]("name", O.DBType("varchar(254)"))
  def value = column[String]("value", O.DBType("varchar(2048)"))
  def level = column[Level]("level", O.NotNull, O.Default(LevelEnum.unsafe))

  override def * = (id.?, envId.?, projectId.?, name, value, level) <> (Variable.tupled, Variable.unapply _)

  def idx = index("idx_name", (envId, projectId, name), unique = true)
}

object VariableHelper extends PlayCache {

  import models.AppDB._

  val qVariable = TableQuery[VariableTable]

  def findByEnvId_ProjectId(envId: Int, projectId: Int): Seq[Variable] = db withSession { implicit session =>
    qVariable.filter(v => v.envId === envId && v.projectId === projectId).list.map { v =>
      v.level match {
        case LevelEnum.safe => v.copy(value = SecurityUtil.decryptUK(v.value))
        case _ => v
      }
    }
  }

  def _create(variables: Seq[Variable])(implicit session: JdbcBackend#Session) = {
    val _variables = variables.map { v =>
      v.level match {
        case LevelEnum.safe => v.copy(value = SecurityUtil.encryptUK(v.value))
        case _ => v
      }
    }
    qVariable.insertAll(_variables: _*)(session)
  }

  def create(variables: Seq[Variable]) = db withSession{ implicit session =>
    _create(variables)(session)
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qVariable.filter(_.projectId === projectId).delete(session)
  }

  def _deleteByEnvId_ProjectId(envId: Int, projectId: Int)(implicit session: JdbcBackend#Session) = {
    qVariable.filter(v => v.envId ===envId && v.projectId === projectId).delete(session)
  }

}
