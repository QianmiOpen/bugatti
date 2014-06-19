package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._
/**
 * 权限
 */
case class Permission(jobNo: String, functions: Option[String])

class PermissionTable(tag: Tag) extends Table[Permission](tag, "permission") {
  def jobNo = column[String]("job_no", O.PrimaryKey, O.DBType("VARCHAR(16)"))
  def functions = column[String]("functions", O.DBType("text"))

  override def * = (jobNo, functions.?) <> (Permission.tupled, Permission.unapply _)
}

object PermissionHelper extends PlayCache {

  import models.AppDB._

  val qPermission = TableQuery[PermissionTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qPermission.where(_.jobNo is jobNo).firstOption
  }

  def create(permission: Permission) = db withSession { implicit session =>
    qPermission.insert(permission)
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    qPermission.where(_.jobNo is jobNo).delete
  }

  def update(jobNo: String, permission: Permission) = db withSession { implicit session =>
    val permission2update = permission.copy(jobNo)
    qPermission.where(_.jobNo is jobNo).update(permission2update)
  }

}