package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException
import models.PlayCache
import play.api.Play.current

import scala.slick.driver.MySQLDriver.simple._
import scala.slick.jdbc.JdbcBackend
/**
 * 环境成员
 *
 * @author of546
 */
case class EnvironmentMember(id: Option[Int], envId: Int, jobNo: String)
class EnvironmentMemberTable(tag: Tag) extends Table[EnvironmentMember](tag, "environment_member") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def envId = column[Int]("env_id")
  def jobNo = column[String]("job_no", O.DBType("VARCHAR(16)"))

  override def * = (id.?, envId, jobNo) <> (EnvironmentMember.tupled, EnvironmentMember.unapply _)
  def idx = index("idx_eid_no", (envId, jobNo), unique = true)
  def idx_eid = index("idx_eid", envId)
}
object EnvironmentMemberHelper extends PlayCache {
  import models.AppDB._

  val qMember = TableQuery[EnvironmentMemberTable]
  val qEnv = TableQuery[EnvironmentTable]

  def findById(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).firstOption
  }

  def findByEnvId(envId: Int): Seq[EnvironmentMember] = db withSession { implicit session =>
    qMember.filter(_.envId === envId).list
  }

  def findByEnvId_JobNo(envId: Int, jobNo: String): Option[EnvironmentMember] = db withSession { implicit session =>
    qMember.filter(m => m.envId === envId && m.jobNo === jobNo).firstOption
  }

  def findByJobNo(jobNo: String): Seq[EnvironmentMember] = db withSession { implicit session =>
    qMember.filter(m => m.jobNo === jobNo).list
  }

  def findEnvsByJobNo(jobNo: String): Seq[Environment] = db withSession { implicit session =>
    val q = for{
      (e, m) <- qEnv innerJoin qMember on (_.id === _.envId)
      if m.jobNo === jobNo
    } yield e
    q.list
  }

  def create(member: EnvironmentMember) = db withSession { implicit session =>
    _create(member)
  }

  @throws[UniqueNameException]
  def _create(member: EnvironmentMember)(implicit session: JdbcBackend#Session) = {
    try {
      qMember.returning(qMember.map(_.id)).insert(member)(session)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

  def delete(id: Int) = db withSession { implicit session =>
    qMember.filter(_.id === id).delete
  }

  def _deleteByEnvId(envId: Int)(implicit session: JdbcBackend#Session) = {
    qMember.filter(_.envId === envId).delete
  }

  @throws[UniqueNameException]
  def update(id: Int, member: EnvironmentMember) = db withSession { implicit session =>
    try {
      val member2update = member.copy(Some(id))
      qMember.filter(_.id === id).update(member2update)
    } catch {
      case x: MySQLIntegrityConstraintViolationException => throw new UniqueNameException
    }
  }

}

