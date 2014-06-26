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
        _.split(",").filterNot(_.isEmpty).map(x => FuncEnum(x.toInt)).toList
    ))
  override def * = (jobNo, functions) <> (Permission.tupled, Permission.unapply _)
}

object PermissionHelper extends PlayCache {

  import models.AppDB._

  val qPermission = TableQuery[PermissionTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qPermission.where(_.jobNo is jobNo).firstOption
  }

  def create(permission: Permission) = db withSession { implicit session =>
    create_(permission)
  }

  def create_(permission: Permission)(implicit session: JdbcBackend#Session) = {
    qPermission.insert(permission)(session)
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    delete_(jobNo)
  }

  def delete_(jobNo: String)(implicit session: JdbcBackend#Session) = {
    qPermission.where(_.jobNo is jobNo).delete(session)
  }

  def update(jobNo: String, permission: Permission) = db withSession { implicit session =>
    val permission2update = permission.copy(jobNo)
    qPermission.where(_.jobNo is jobNo).update(permission2update)
  }

  def update_(jobNo: String, permission: Permission)(implicit session: JdbcBackend#Session) = {
    val permission2update = permission.copy(jobNo)
    qPermission.where(_.jobNo is jobNo).update(permission2update)(session)
  }

}