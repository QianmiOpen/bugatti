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
case class Member(id: Option[Int], pid: Int, level: Level, jobNo: String)
class MemberTable(tag: Tag) extends Table[Member](tag, "member") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pid = column[Int]("pid", O.NotNull) // 项目编号
  def level = column[Level]("level", O.NotNull, O.Default(LevelEnum.unsafe)) // 成员级别(对应环境级别)
  def jobNo = column[String]("job_no", O.NotNull, O.DBType("VARCHAR(16)"))

  override def * = (id.?, pid, level, jobNo) <> (Member.tupled, Member.unapply _)
  def idx = index("idx_pid_no", (pid, jobNo), unique = true)
  def idx_pid = index("idx_pid", pid)
}
object MemberHelper extends PlayCache {

  import models.AppDB._

  val qMember = TableQuery[MemberTable]

  def findById(id: Int) = db withSession { implicit session =>
    qMember.where(_.id is id).firstOption
  }

  def findByPid(pid: Int): Seq[Member] = db withSession { implicit session =>
    qMember.where(m => m.pid === pid).list
  }

  def findByPid_JobNo(pid: Int, jobNo: String): Option[Member] = db withSession { implicit session =>
    qMember.where(m => m.pid === pid && m.jobNo === jobNo).firstOption
  }

  def create(member: Member) = db withSession { implicit session =>
    create_(member)
  }

  def create_(member: Member)(implicit session: JdbcBackend#Session) = {
    qMember.insert(member)(session)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qMember.where(_.id is id).delete
  }

  def update(id: Int, member: Member) = db withSession { implicit session =>
    val member2update = member.copy(Some(id))
    qMember.where(_.id is id).update(member2update)
  }

}