package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * Created by mind on 1/8/15.
 */

case class Spirit(id: Option[Int], name: String, ip: String)

case class SpiritTable(tag: Tag) extends Table[Spirit](tag, "spirit") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name", O.DBType("VARCHAR(200)"))
  def ip = column[String]("ip", O.DBType("VARCHAR(16)"))

  override def * = (id.?, name, ip) <> (Spirit.tupled, Spirit.unapply _)

  index("idx_ip", ip, unique = true)
}

object SpiritHelper {
  import models.AppDB._

  val qSpirit = TableQuery[SpiritTable]

  def all: Seq[Spirit] = db withSession { implicit session =>
    qSpirit.list
  }

  def create(spirit: Spirit): Int = db withSession { implicit session =>
      qSpirit.returning(qSpirit.map(_.id)).insert(spirit)
  }

  def delete(id: Int) = db withSession { implicit session =>
    qSpirit.filter(_.id === id).delete
  }

  def update(spirit: Spirit) = db withSession { implicit session =>
    qSpirit.filter(_.id === spirit.id).update(spirit)
  }

  def findById(id: Int): Option[Spirit] = db withSession { implicit session =>
    qSpirit.filter(_.id === id).firstOption
  }
}
