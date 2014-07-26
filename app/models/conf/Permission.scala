package models.conf

import play.api.Play.current
import models.PlayCache
import enums.FuncEnum._
import enums.FuncEnum

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 权限
 *
 * @author of546
 */
case class Permission(jobNo: String, functions: Seq[Func])

class PermissionTable(tag: Tag) extends Table[Permission](tag, "permission") {

  def jobNo = column[String]("job_no", O.PrimaryKey, O.DBType("VARCHAR(16)"))
  def functions = column[Seq[Func]]("functions", O.DBType("text"))(MappedColumnType.base[Seq[Func], String]
    (
        _.map(_.id).mkString(","),
        _.split(",").filterNot(_.trim.isEmpty).map(x => FuncEnum(x.toInt)).toList
    ))
  override def * = (jobNo, functions) <> (Permission.tupled, Permission.unapply _)
}

object PermissionHelper extends PlayCache {

  import models.AppDB._

  val qPermission = TableQuery[PermissionTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qPermission.filter(_.jobNo === jobNo).firstOption
  }

  def create(permission: Permission) = db withSession { implicit session =>
    _create(permission)
  }

  def _create(permission: Permission)(implicit session: JdbcBackend#Session) = {
    qPermission.insert(permission)(session)
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    _delete(jobNo)
  }

  def _delete(jobNo: String)(implicit session: JdbcBackend#Session) = {
    qPermission.filter(_.jobNo === jobNo).delete(session)
  }

  def update(jobNo: String, permission: Permission) = db withSession { implicit session =>
    _update(jobNo, permission)
  }

  def _update(jobNo: String, permission: Permission)(implicit session: JdbcBackend#Session) = {
    val permission2update = permission.copy(jobNo)
    qPermission.filter(_.jobNo === jobNo).update(permission2update)(session)
  }

}