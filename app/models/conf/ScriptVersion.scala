package models.conf

import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * Created by mind on 7/23/14.
 */
case class ScriptVersion(id: Option[Int], name: String, updateTime: DateTime = new DateTime, message: Option[String])

case class ScriptVersionTable(tag: Tag) extends Table[ScriptVersion](tag, "script_version") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def updateTime = column[DateTime]("update_time", O.Default(DateTime.now))
  def message = column[String]("message", O.Nullable)

  override  def * = (id.?, name, updateTime, message.?) <> (ScriptVersion.tupled, ScriptVersion.unapply _)

  index("idx_name", name, unique = true)
}

object ScriptVersionHelper {
  val Latest = "latest"
  val Master = "master"

  import models.AppDB._
  val qScriptVersion = TableQuery[ScriptVersionTable]

  def create(scriptVersion: ScriptVersion) = db withSession { implicit session =>
    qScriptVersion.returning(qScriptVersion.map(_.id)).insert(scriptVersion)
  }

  def all(): Seq[ScriptVersion] = db withSession { implicit session =>
    qScriptVersion.list()
  }

  def allName(): Seq[String] = db withSession { implicit session =>
    all().map(_.name)
  }

  def deleteByName(name: String) = db withSession { implicit session =>
    qScriptVersion.filter(_.name === name).delete
  }

  def findLatest(): Option[String] = db withSession { implicit session =>
    Query(qScriptVersion.map(_.name).max).first
  }
}
