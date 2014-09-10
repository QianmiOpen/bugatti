package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException
import play.api.Play.current
import enums.{LevelEnum, FuncEnum, RoleEnum}
import enums.RoleEnum.Role
import models.{MaybeFilter, PlayCache}
import org.joda.time.DateTime

import scala.slick.driver.MySQLDriver.simple._
import com.github.tototoshi.slick.MySQLJodaSupport._

import scala.slick.jdbc.JdbcBackend

/**
 * 用户
 *
 * @author of546
 */
case class User(jobNo: String, name: String, role: Role, superAdmin: Boolean, locked: Boolean, lastIp: Option[String], lastVisit: Option[DateTime])
case class UserForm(jobNo: String, name: String, role: Role, superAdmin: Boolean, locked: Boolean, lastIp: Option[String], lastVisit: Option[DateTime], functions: String) {
  def toUser = User(jobNo.toLowerCase, name, role, superAdmin, locked, lastIp, lastVisit)
  def toPermission = Permission(jobNo.toLowerCase, functions.split(",").filterNot(_.isEmpty).map(i => FuncEnum(i.toInt)).toList)
}

class UserTable(tag: Tag) extends Table[User](tag, "app_user") {
  def jobNo = column[String]("job_no", O.PrimaryKey, O.DBType("VARCHAR(16)"))
  def name = column[String]("name", O.DBType("VARCHAR(20)"))
  def role = column[Role]("role", O.Default(RoleEnum.user), O.DBType("ENUM('admin', 'user')")) // 用户角色
  def superAdmin = column[Boolean]("super_admin", O.Default(false), O.DBType("ENUM('y', 'n')"))(MappedColumnType.base[Boolean, String](if(_) "y" else "n",  _ == "y")) // 超级管理员
  def locked = column[Boolean]("locked", O.Default(false), O.DBType("ENUM('y', 'n')"))(MappedColumnType.base[Boolean, String](if(_) "y" else "n",  _ == "y")) // 账号锁定
  def lastIp = column[String]("last_ip", O.Nullable, O.DBType("VARCHAR(40)")) // 最近登录ip
  def lastVisit = column[DateTime]("last_visit", O.Nullable, O.Default(DateTime.now())) // 最近登录时间

  override def * = (jobNo, name, role, superAdmin, locked, lastIp.?, lastVisit.?) <> (User.tupled, User.unapply _)

  def idx = index("idx_job_no", jobNo)
}

object UserHelper extends PlayCache {

  import models.AppDB._

  val qUser = TableQuery[UserTable]

  def findByJobNo(jobNo: String) = db withSession { implicit session =>
    qUser.filter(_.jobNo === jobNo).firstOption
  }

  def count(jobNo: Option[String]): Int = db withSession { implicit session =>
    val query = MaybeFilter(qUser).filter(jobNo)(v => b => b.jobNo === v).query
    query.length.run
  }

  def all(jobNo: Option[String], page: Int, pageSize: Int): Seq[User] = db withSession { implicit session =>
    val offset = pageSize * page
    val query = MaybeFilter(qUser).filter(jobNo)(v => b => b.jobNo === v).query
    query.drop(offset).take(pageSize).list
  }

  def create(user: User) = db withSession { implicit session =>
    _create(user)
  }

  def create(user: User, permission: Permission) = db withTransaction { implicit session =>
    _create(user) + PermissionHelper._create(permission)
  }

  @throws[UniqueNameException]
  def _create(user: User)(implicit session: JdbcBackend#Session) = {
    try {
      qUser.insert(user)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def delete(jobNo: String) = db withSession { implicit session =>
    _delete(jobNo) + PermissionHelper.delete(jobNo)
  }

  def _delete(jobNo: String)(implicit session: JdbcBackend#Session) = {
    qUser.filter(_.jobNo === jobNo).delete(session)
  }

  def update(jobNo: String, user: User) = db withSession { implicit session =>
    _update(jobNo, user)
  }

  def update(jobNo: String, user: User, permission: Permission) = db withTransaction { implicit session =>
    _update(jobNo, user) + (PermissionHelper.findByJobNo(jobNo) match {
      case Some(_) =>
        PermissionHelper._update(jobNo, permission)
      case None =>
        PermissionHelper._create(permission)
    })
  }

  @throws[UniqueNameException]
  def _update(jobNo: String, user: User)(implicit session: JdbcBackend#Session) = {
//    val user2update = user.copy(jobNo) // bug
    try {
      qUser.filter(_.jobNo === jobNo).update(user)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  // ---------------------------------------------------
  // 项目和环境资源权限, todo add cache
  // ---------------------------------------------------

  /* 项目委员 */
  def hasProject(projectId: Int, user: User): Boolean = {
    if (superAdmin_?(user)) true
    else ProjectMemberHelper.findByProjectId_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.projectId == projectId => true
      case _ => false
    }
  }

  /* 项目委员长 */
  def hasProjectSafe(projectId: Int, user: User): Boolean = {
    if (superAdmin_?(user)) true
    else ProjectMemberHelper.findByProjectId_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.projectId == projectId && member.level == LevelEnum.safe => true
      case _ => false
    }
  }

  /* 指定环境下，根据安全级别选择委员长或成员访问 */
  def hasProjectInEnv(projectId: Int, envId: Int, user: User): Boolean = {
    if (superAdmin_?(user)) true
    else ProjectMemberHelper.findByProjectId_JobNo(projectId, user.jobNo) match {
      case Some(member) if member.projectId == projectId =>
        EnvironmentHelper.findById(envId) match {
          case Some(env) if env.level == LevelEnum.safe => if (member.level == env.level) true else false
          case Some(env) if env.level == LevelEnum.unsafe => true
          case _ => false
        }
      case _ => false
    }
  }

  /* 环境成员 */
  def hasEnv(envId: Int, user: User): Boolean = {
    if (superAdmin_?(user)) true
    else EnvironmentHelper.findById(envId) match {
      case Some(env) if env.jobNo == Some(user.jobNo) => true
      case Some(env) if env.jobNo != Some(user.jobNo) =>
        EnvironmentMemberHelper.findEnvId_JobNo(envId, user.jobNo) match {
          case Some(_) => true
          case _ => false
        }
      case _ => false
    }
  }

  def superAdmin_?(user: User): Boolean = if (user.role == RoleEnum.admin && user.superAdmin) true else false

}