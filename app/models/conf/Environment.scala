package models.conf

import play.api.Play.current
import enums.LevelEnum
import enums.LevelEnum.Level
import scala.slick.driver.MySQLDriver.simple._
/**
 * 环境
 *
 * @author of546
 */
case class Environment(id: Option[Int], name: String, remark: Option[String], nfServer: Option[String], ipRange: Option[String], level: Level)
class EnvironmentTable(tag: Tag) extends Table[Environment](tag, "environment") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull, O.DBType("VARCHAR(30)"))
  def nfServer = column[String]("nfs_server", O.Nullable, O.DBType("VARCHAR(30)"))
  def ipRange = column[String]("ip_range", O.Nullable, O.DBType("VARCHAR(300)"))
  def remark = column[String]("remark", O.Nullable)
  def level = column[Level]("level", O.NotNull, O.Default(LevelEnum.unsafe)) // 项目安全级别，默认为公共的。

  override def * = (id.?, name, remark.?, nfServer.?, ipRange.?, level) <> (Environment.tupled, Environment.unapply _)
  def idx = index("idx_name", name, unique = true)
}
object EnvironmentHelper {

  import models.AppDB._

  val qEnvironment = TableQuery[EnvironmentTable]

  def findById(id: Int): Option[Environment] = db withSession { implicit session =>
    qEnvironment.where(_.id is id).firstOption
  }

  def findByName(name: String): Option[Environment] = db withSession { implicit session =>
    qEnvironment.where(_.name is name).firstOption
  }

  def count: Int = db withSession { implicit session =>
    Query(qEnvironment.length).first
  }

  def all(page: Int, pageSize: Int): Seq[Environment] = db withSession { implicit session =>
    val offset = pageSize * page
    qEnvironment.drop(offset).take(pageSize).list
  }

  def all(): Seq[Environment] = db withSession { implicit session =>
    qEnvironment.list
  }

  def create(environment: Environment) = db withSession { implicit session =>
    qEnvironment.insert(environment)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qEnvironment.where(_.id is id).delete
  }

  def update(id: Int, env: Environment) = db withSession { implicit session =>
    val envToUpdate = env.copy(Some(id))
    qEnvironment.where(_.id is id).update(envToUpdate)
  }

}