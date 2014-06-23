package models.conf

import play.api.Play.current
import enums.RoleEnum
import enums.RoleEnum.Role
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

/**
 * 用户
 *
 * @author of546
 */
case class User(jobNo: String, name: Option[String], role: Option[Role], locked: Option[Boolean], lastIp: Option[String], lastVisit: Option[DateTime])

class UserTable(tag: Tag) extends Table[User](tag, "app_user") {
  def jobNo = column[String]("job_no", O.PrimaryKey, O.DBType("VARCHAR(16)"))
  def name = column[String]("name", O.Nullable, O.DBType("VARCHAR(20)"))
  def role = column[Role]("role", O.NotNull, O.Default(RoleEnum.user), O.DBType("ENUM('admin', 'user')")) // 用户角色
  def locked = column[Boolean]("locked", O.Default(false), O.DBType("ENUM('y', 'n')"))(MappedColumnType.base[Boolean, String](if(_) "y" else "n",  _ == "y")) // 账号锁定
  def lastIp = column[String]("last_ip", O.DBType("VARCHAR(40)")) // 最近登录ip
  def lastVisit = column[DateTime]("last_visit", O.Default(DateTime.now())) // 最近登录时间

  override def * = (jobNo, name.?, role.?, locked.?, lastIp.?, lastVisit.?) <> (User.tupled, User.unapply _)
}

object UserHelper extends PlayCache {

  import models.AppDB._

  val qUser = TableQuery[UserTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qUser.where(_.jobNo is jobNo).firstOption
  }

  def create(user: User) = db withSession { implicit session =>
    qUser.insert(user)
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    qUser.where(_.jobNo is jobNo).delete
  }

  def update(jobNo: String, user: User) = db withSession { implicit session =>
    val user2update = user.copy(jobNo)
    qUser.where(_.jobNo is jobNo).update(user2update)
  }

}