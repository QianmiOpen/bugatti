package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException
import play.api.Play.current
import models.PlayCache
import enums.LevelEnum
import enums.LevelEnum.Level

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 项目成员
 *
 * @author of546
 */
case class ProjectMember(id: Option[Int], projectId: Int, level: Level, jobNo: String)
class ProjectMemberTable(tag: Tag) extends Table[ProjectMember](tag, "project_member") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id") // 项目编号
  def level = column[Level]("level", O.Default(LevelEnum.unsafe)) // 成员级别(对应环境级别)
  def jobNo = column[String]("job_no", O.DBType("VARCHAR(16)"))

  override def * = (id.?, projectId, level, jobNo) <> (ProjectMember.tupled, ProjectMember.unapply _)
  def idx = index("idx_pid_no", (projectId, jobNo), unique = true)
  def idx_pid = index("idx_pid", projectId)
}
object ProjectMemberHelper extends PlayCache {

  import models.AppDB._

  val qMember = TableQuery[ProjectMemberTable]
  val qProject = TableQuery[ProjectTable]

  def findById(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).firstOption
  }

  def findByProjectId(projectId: Int): Seq[ProjectMember] = db withSession { implicit session =>
    qMember.filter(_.projectId === projectId).list
  }

  def findByProjectId_JobNo(projectId: Int, jobNo: String): Option[ProjectMember] = db withSession { implicit session =>
    qMember.filter(m => m.projectId === projectId && m.jobNo === jobNo).firstOption
  }

  def findProjectsByJobNo(jobNo: String): Seq[Project] = db withSession { implicit session =>
    val q = for{
      (m, p) <- qMember innerJoin qProject on (_.projectId === _.id)
      if m.jobNo === jobNo
    } yield p
    q.sortBy(_.name).list
  }

  def findSafeProjectsByJobNo(jobNo: String): Seq[Project] = db withSession { implicit session =>
    val q = for{
      (m, p) <- qMember innerJoin qProject on (_.projectId === _.id)
      if m.jobNo === jobNo && m.level === LevelEnum.safe
    } yield p
    q.sortBy(_.name).list
  }

  def count(jobNo: String, level: Level): Int = db withSession { implicit session =>
    qMember.filter(m => m.jobNo === jobNo && m.level === level ).length.run
  }

  def create(member: ProjectMember) = db withSession { implicit session =>
    _create(member)
  }

  @throws[UniqueNameException]
  def _create(member: ProjectMember)(implicit session: JdbcBackend#Session) = {
    try {
      qMember.returning(qMember.map(_.id)).insert(member)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def delete(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).delete
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qMember.filter(_.projectId === projectId).delete
  }

  @throws[UniqueNameException]
  def update(id: Int, member: ProjectMember) = db withSession { implicit session =>
    try {
      val member2update = member.copy(Some(id))
      qMember.filter(_.id === id).update(member2update)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

}