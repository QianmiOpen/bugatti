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
  val Master = "master"

  import models.AppDB._
  val qScriptVersion = TableQuery[ScriptVersionTable]

  def all(): Seq[ScriptVersion] = db withSession { implicit session =>
    qScriptVersion.list()
  }

  def allName(): Seq[String] = db withSession { implicit session =>
    all().map(_.name)
  }

  def isSameBranch(versionName: String, branchId: String): Boolean = db withSession  { implicit session =>
    qScriptVersion.filter(x => x.name === versionName && x.message === branchId).length.run > 0
  }

  def updateVersionByName(scriptVersion: ScriptVersion) = db withSession { implicit session =>
    qScriptVersion.filter(_.name === scriptVersion.name).firstOption match {
      case Some(tsv) =>
        qScriptVersion.filter(_.id === tsv.id).update(tsv.copy(message = scriptVersion.message, updateTime = scriptVersion.updateTime))
      case None =>
        qScriptVersion.returning(qScriptVersion.map(_.id)).insert(scriptVersion)
    }
  }

}
