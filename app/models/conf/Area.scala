package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by mind on 7/6/14.
 */
case class AreaInfo(id: Option[Int], name: String, syndicName: String, syndicIp: String, total: Int, envNCount: Int, projectNCount: Int)

case class Area(id: Option[Int], name: String, syndicName: String, syndicIp: String)

case class AreaTable(tag: Tag) extends Table[Area](tag, "area") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def syndicName = column[String]("syndic_name", O.NotNull)
  def syndicIp = column[String]("syndic_ip", O.NotNull)

  override def * = (id.?, name, syndicName, syndicIp) <>(Area.tupled, Area.unapply _)

  index("idx_name", name, unique = true)
  index("idx_syndic_name", syndicName, unique = true)
}

object AreaHelper {
  import models.AppDB._

  val qArea = TableQuery[AreaTable]
  val qRel = TableQuery[EnvironmentProjectRelTable]

  def create(area: Area): Int = db withSession { implicit session =>
    qArea.returning(qArea.map(_.id)).insert(area)
  }

  def findByName(name: String): Option[Area] = db withSession { implicit session =>
    qArea.where(_.name === name).firstOption
  }

  def update(area: Area) = db withSession { implicit session =>
    qArea.where(_.id === area.id).update(area)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qArea.where(_.id === id).delete
  }

  def findById(id: Int): Option[Area] = db withSession { implicit session =>
    qArea.where(_.id === id).firstOption
  }

  def allInfo: Seq[AreaInfo] = db withSession { implicit session =>
    qArea.list().map(_Area2AreaInfo)
  }

  def findInfoById(id: Int): Option[AreaInfo] = db withSession { implicit session =>
    qArea.where(_.id === id).firstOption.map(_Area2AreaInfo)
  }

  def _Area2AreaInfo(implicit session: Session) = { area: Area =>
    val syndicName = area.syndicName
    val counts = (qRel.where(_.syndicName === syndicName).length.run,
      qRel.where(c => c.syndicName === syndicName && c.envId.isNull).length.run,
      qRel.where(c => c.syndicName === syndicName && c.projectId.isNull).length.run)
    AreaInfo(area.id, area.name, area.syndicName, area.syndicIp, counts._1, counts._2, counts._3)
  }
}