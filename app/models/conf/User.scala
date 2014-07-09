package models.conf

import play.api.Play.current
import enums.{LevelEnum, FuncEnum, RoleEnum}
import enums.RoleEnum.Role
import models.PlayCache
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

import scala.slick.jdbc.JdbcBackend

/**
 * 用户
 *
 * @author of546
 */
case class User(jobNo: String, name: String, role: Role, locked: Boolean, lastIp: Option[String], lastVisit: Option[DateTime])
case class UserForm(jobNo: String, name: String, role: Role, locked: Boolean, lastIp: Option[String], lastVisit: Option[DateTime], functions: String) {
  def toUser = User(jobNo, name, role, locked, lastIp, lastVisit)
  def toPermission = Permission(jobNo, functions.split(",").filterNot(_.isEmpty).map(i => FuncEnum(i.toInt)).toList)
}

class UserTable(tag: Tag) extends Table[User](tag, "app_user") {
  def jobNo = column[String]("job_no", O.PrimaryKey, O.DBType("VARCHAR(16)"))
  def name = column[String]("name", O.Nullable, O.DBType("VARCHAR(20)"))
  def role = column[Role]("role", O.NotNull, O.Default(RoleEnum.user), O.DBType("ENUM('admin', 'user')")) // 用户角色
  def locked = column[Boolean]("locked", O.Default(false), O.DBType("ENUM('y', 'n')"))(MappedColumnType.base[Boolean, String](if(_) "y" else "n",  _ == "y")) // 账号锁定
  def lastIp = column[String]("last_ip", O.Nullable, O.DBType("VARCHAR(40)")) // 最近登录ip
  def lastVisit = column[DateTime]("last_visit", O.Nullable, O.Default(DateTime.now())) // 最近登录时间

  override def * = (jobNo, name, role, locked, lastIp.?, lastVisit.?) <> (User.tupled, User.unapply _)
}

object UserHelper extends PlayCache {

  import models.AppDB._

  val qUser = TableQuery[UserTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qUser.where(_.jobNo is jobNo).firstOption
  }

  def count: Int = db withSession { implicit session =>
    Query(qUser.length).first
  }

  def all(page: Int, pageSize: Int): Seq[User] = db withSession { implicit session =>
    val offset = pageSize * page
    qUser.drop(offset).take(pageSize).list
  }

  def create(user: User) = db withSession { implicit session =>
    create_(user)
  }

  def create_(user: User)(implicit session: JdbcBackend#Session) = {
    qUser.insert(user)(session)
  }

  def create(user: User, permission: Permission) = db withTransaction { implicit session =>
    create_(user) + PermissionHelper.create_(permission)
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    delete_(jobNo) + PermissionHelper.delete(jobNo)
  }

  def delete_(jobNo: String)(implicit session: JdbcBackend#Session) = {
    qUser.where(_.jobNo is jobNo).delete(session)
  }

  def update(jobNo: String, user: User) = db withSession { implicit session =>
    update_(jobNo, user)
  }

  def update_(jobNo: String, user: User)(implicit session: JdbcBackend#Session) = {
    val user2update = user.copy(jobNo)
    qUser.where(_.jobNo is jobNo).update(user2update)(session)
  }

  def update(jobNo: String, user: User, permission: Permission) = db withTransaction { implicit session =>
    update_(jobNo, user) + (PermissionHelper.findByJobNo(jobNo) match {
      case Some(p) =>
        PermissionHelper.update_(jobNo, permission)
      case None =>
        PermissionHelper.create_(permission)
    })
  }

  // ---------------------------------------------------
  // 项目和环境资源权限
  // ---------------------------------------------------
  def hasProject(projectId: Int, user: User): Boolean = {
    if (user.role == RoleEnum.admin) true
    else MemberHelper.findByPid_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.pid == projectId => true
      case _ => false
    }
  }

  def hasProjectSafe(projectId: Int, user: User): Boolean = {
    if (user.role == RoleEnum.admin) true
    else MemberHelper.findByPid_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.pid == projectId && member.level == LevelEnum.safe => true
      case _ => false
    }
  }

  def hasProjectInEnv(projectId: Int, envId: Int, user: User): Boolean = {
    if (user.role == RoleEnum.admin) true
    else MemberHelper.findByPid_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.pid == projectId =>
        EnvironmentHelper.findById(envId) match {
          case Some(env) if env.level == LevelEnum.safe => if (member.level == env.level) true else false
          case Some(env) if env.level == LevelEnum.unsafe => true
          case None => false
        }
      case _ => false
    }
  }

}