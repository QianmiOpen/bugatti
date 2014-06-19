package models.conf

import play.api.Play.current
import models.PlayCache

import scala.slick.driver.MySQLDriver.simple._

/**
 * 资源成员
 * CREATE TABLE `member` (
 *  `id` INT
 *  `pid` INT
 *  `level`
 *  `job_no`
 * )
 */
case class Member(id: Int, pid: Int, level: Int, job_no: String)
class MemberTable(tag: Tag) extends Table[Member](tag, "member") {
  def id = column[Int]("id", O.PrimaryKey)
  def pid = column[Int]("pid")
  def level = column[Int]("level")
  def jobNo = column[String]("job_no", O.DBType("VARCHAR(16)"))

  override def * = (id, pid, level, jobNo) <> (Member.tupled, Member.unapply _)
}
object MemberHelper extends PlayCache {

  import models.AppDB._

  val qMember = TableQuery[MemberTable]

  def findById(id: Int) = db withSession { implicit session =>
    qMember.where(_.id is id).firstOption
  }

  def create(member: Member) = db withSession { implicit session =>
    qMember.insert(member)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qMember.where(_.id is id).delete
  }

  def update(id: Int, member: Member) = db withSession { implicit session =>
    val member2update = member.copy(id)
    qMember.where(_.id is id).update(member2update)
  }
}