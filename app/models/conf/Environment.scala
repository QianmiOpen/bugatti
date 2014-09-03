package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException
import play.api.Play.current
import enums.LevelEnum
import enums.LevelEnum.Level
import scala.slick.driver.MySQLDriver.simple._
/**
 * 环境
 *
 * @author of546
 */
case class Environment(id: Option[Int], name: String, remark: Option[String], nfServer: Option[String], ipRange: Option[String], level: Level, scriptVersion: String = ScriptVersionHelper.Latest)
class EnvironmentTable(tag: Tag) extends Table[Environment](tag, "environment") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.DBType("VARCHAR(30)"))
  def nfServer = column[String]("nfs_server", O.Nullable, O.DBType("VARCHAR(30)"))
  def ipRange = column[String]("ip_range", O.Nullable, O.DBType("VARCHAR(300)"))
  def remark = column[String]("remark", O.Nullable)
  def level = column[Level]("level", O.Default(LevelEnum.unsafe)) // 项目安全级别，默认为公共的。
  def scriptVersion = column[String]("script_version", O.Default(ScriptVersionHelper.Latest), O.DBType("VARCHAR(60)")) // 环境使用salt script的版本

  override def * = (id.?, name, remark.?, nfServer.?, ipRange.?, level, scriptVersion) <> (Environment.tupled, Environment.unapply _)
  def idx = index("idx_name", name, unique = true)
}
object EnvironmentHelper {
  import models.AppDB._
  val qEnvironment = TableQuery[EnvironmentTable]

  def findById(id: Int): Option[Environment] = db withSession { implicit session =>
    qEnvironment.filter(_.id === id).firstOption
  }

  def findByUnsafe(): Seq[Environment] = db withSession { implicit session =>
    qEnvironment.filter(_.level === LevelEnum.unsafe).list
  }

  def findByName(name: String): Option[Environment] = db withSession { implicit session =>
    qEnvironment.filter(_.name === name).firstOption
  }

  def count: Int = db withSession { implicit session =>
    qEnvironment.length.run
  }

  def all(page: Int, pageSize: Int): Seq[Environment] = db withSession { implicit session =>
    val offset = pageSize * page
    qEnvironment.drop(offset).take(pageSize).list
  }

  def all(): Seq[Environment] = db withSession { implicit session =>
    qEnvironment.list
  }

  @throws[UniqueNameException]
  def create(environment: Environment) = db withSession { implicit session =>
    try {
      qEnvironment.returning(qEnvironment.map(_.id)).insert(environment)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def delete(id: Int) = db withSession { implicit session =>
    qEnvironment.filter(_.id === id).delete
  }

  @throws[UniqueNameException]
  def update(id: Int, env: Environment) = db withSession { implicit session =>
    try {
      val envToUpdate = env.copy(Some(id))
      qEnvironment.filter(_.id === id).update(envToUpdate)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

}