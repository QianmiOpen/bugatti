package models.conf

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import exceptions.UniqueNameException

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * 区域
 *
 * @author 557
 */
case class AreaInfo(id: Option[Int], name: String, syndicName: String, syndicIp: String, total: Int, envNCount: Int, projectNCount: Int)

case class Area(id: Option[Int], name: String, syndicName: String, syndicIp: String)

case class AreaTable(tag: Tag) extends Table[Area](tag, "area") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def syndicName = column[String]("syndic_name")
  def syndicIp = column[String]("syndic_ip")

  override def * = (id.?, name, syndicName, syndicIp) <>(Area.tupled, Area.unapply _)

  index("idx_name", name, unique = true)
}

object AreaHelper {
  import models.AppDB._

  val qArea = TableQuery[AreaTable]
  val qRel = TableQuery[EnvironmentProjectRelTable]

  def all: Seq[Area] = db withSession { implicit session =>
    qArea.list()
  }

  def findById(id: Int): Option[Area] = db withSession { implicit session =>
    qArea.filter(_.id === id).firstOption
  }

  def findBySyndicName(name: String): Option[Area] = db withSession { implicit session =>
    qArea.filter(_.syndicName === name).firstOption
  }

  def allInfo: Seq[AreaInfo] = db withSession { implicit session =>
    qArea.list().map(_Area2AreaInfo)
  }

  def findInfoById(id: Int): Option[AreaInfo] = db withSession { implicit session =>
    qArea.filter(_.id === id).firstOption.map(_Area2AreaInfo)
  }

  @throws[UniqueNameException]
  def create(area: Area): Int = db withSession { implicit session =>
    try {
      qArea.returning(qArea.map(_.id)).insert(area)
    } catch {
      case x: MySQLIntegrityConstraintViolationException =>
        val col_index = x.getMessage match {
          case msg if msg endsWith ("'idx_name'") => "1"
          case _ => "2"
        }
        throw new UniqueNameException(col_index)
    }
  }

  def delete(id: Int) = db withSession { implicit session =>
    qArea.filter(_.id === id).delete
  }

  @throws[UniqueNameException]
  def update(area: Area) = db withSession { implicit session =>
    try {
      qArea.filter(_.id === area.id).update(area)
    } catch {
      case x: MySQLIntegrityConstraintViolationException =>
        val col_index = x.getMessage match {
          case msg if msg endsWith ("'idx_name'") => "1"
          case _ => "2"
        }
        throw new UniqueNameException(col_index)
    }
  }

  def _Area2AreaInfo(implicit session: Session) = { area: Area =>
    val syndicName = area.syndicName
    val counts = (qRel.filter(_.syndicName === syndicName).length.run,
      qRel.filter(c => c.syndicName === syndicName && c.envId.isNull).length.run,
      qRel.filter(c => c.syndicName === syndicName && c.projectId.isNull).length.run)
    AreaInfo(area.id, area.name, area.syndicName, area.syndicIp, counts._1, counts._2, counts._3)
  }

}