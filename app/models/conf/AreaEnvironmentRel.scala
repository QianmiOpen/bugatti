package models.conf

import scala.slick.driver.MySQLDriver.simple._
import play.api.Play.current

/**
 * 区域环境关系
 */
case class AreaEnvironmentRel(id: Option[Int], areaId: Option[Int], envId: Option[Int], ipRange: Option[String])
class AreaEnvironmentRelTable(tag: Tag) extends Table[AreaEnvironmentRel](tag, "area_environment_rel") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def areaId = column[Int]("area_id", O.Nullable)
  def envId = column[Int]("env_id", O.Nullable)
  def ipRange = column[String]("ip_range", O.Nullable, O.DBType("VARCHAR(300)"))

  override def * = (id.?, areaId.?, envId.?, ipRange.?) <> (AreaEnvironmentRel.tupled, AreaEnvironmentRel.unapply _)
  def idx = index("idx_area_env", (areaId, envId), unique = true)
}
object AreaEnvironmentRelHelper {
  import models.AppDB._
  val qRel = TableQuery[AreaEnvironmentRelTable]
  val qArea = TableQuery[AreaTable]

  def findById(id: Int): Option[AreaEnvironmentRel] = db withSession { implicit session =>
    qRel.filter(_.id === id).firstOption
  }

  def findAreasByEnvId(envId: Int): Seq[Area] = db withSession { implicit session =>
    val q = for {
      r <- qRel if r.envId === envId
      a <- qArea if a.id === r.areaId
    } yield a
    q.list
  }

  def findByAreaId_EnvId(areaId: Int, envId: Int): Seq[AreaEnvironmentRel] = db withSession { implicit session =>
    qRel.filter(r => r.areaId === areaId && r.envId === envId).list
  }

}
