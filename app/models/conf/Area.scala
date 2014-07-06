package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by mind on 7/6/14.
 */
case class Area(id: Option[Int], name: String, syndicName: String, syndicIp: String)
case class AreaTable(tag: Tag) extends Table[Area](tag, "area"){
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.NotNull)
  def syndicName = column[String]("syndic_name", O.NotNull)
  def syndicIp = column[String]("syndic_ip", O.NotNull)

  override def * = (id.?, name, syndicName, syndicIp) <> (Area.tupled, Area.unapply _)
}

object AreaHelper {
  import models.AppDB._
  val qArea = TableQuery[AreaTable]

  def create(area: Area): Int = db withSession { implicit session =>
    qArea.returning(qArea.map(_.id)).insert(area)
  }

  def all : Seq[Area] = db withSession { implicit session =>
    qArea.list()
  }
}