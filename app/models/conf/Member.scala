package models.conf

import play.api.Play.current
import models.PlayCache
import enums.LevelEnum
import enums.LevelEnum.Level

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend

/**
 * 资源成员
 *
 * @author of546
 */
case class Member(id: Option[Int], projectId: Int, level: Level, jobNo: String)
class MemberTable(tag: Tag) extends Table[Member](tag, "member") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def projectId = column[Int]("project_id") // 项目编号
  def level = column[Level]("level", O.Default(LevelEnum.unsafe)) // 成员级别(对应环境级别)
  def jobNo = column[String]("job_no", O.DBType("VARCHAR(16)"))

  override def * = (id.?, projectId, level, jobNo) <> (Member.tupled, Member.unapply _)
  def idx = index("idx_pid_no", (projectId, jobNo), unique = true)
  def idx_pid = index("idx_pid", projectId)
}
object MemberHelper extends PlayCache {

  import models.AppDB._

  val qMember = TableQuery[MemberTable]
  val qProject = TableQuery[ProjectTable]

  def findById(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).firstOption
  }

  def findByProjectId(projectId: Int): Seq[Member] = db withSession { implicit session =>
    qMember.filter(m => m.projectId === projectId).list
  }

  def findByProjectId_JobNo(projectId: Int, jobNo: String): Option[Member] = db withSession { implicit session =>
    qMember.filter(m => m.projectId === projectId && m.jobNo === jobNo).firstOption
  }

  def findProjectsByJobNo(jobNo: String): Seq[Project] = db withSession { implicit session =>
    val q = for{
      (m, p) <- qMember leftJoin qProject on (_.projectId === _.id)
      if m.jobNo === jobNo
    } yield p
    q.list
  }

  def count(jobNo: String, level: Level): Int = db withSession { implicit session =>
    qMember.filter(m => m.jobNo === jobNo && m.level === level ).length.run
  }

  def create(member: Member) = db withSession { implicit session =>
    _create(member)
  }

  def _create(member: Member)(implicit session: JdbcBackend#Session) = {
    qMember.returning(qMember.map(_.id)).insert(member)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).delete
  }

  def _deleteByProjectId(projectId: Int)(implicit session: JdbcBackend#Session) = {
    qMember.filter(_.projectId === projectId).delete
  }

  def update(id: Int, member: Member) = db withSession { implicit session =>
    val member2update = member.copy(Some(id))
    qMember.filter(_.id === id).update(member2update)
  }

}