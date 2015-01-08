package models


import play.api.Application
import scala.slick.driver.MySQLDriver.simple._
import play.api.cache.Cache
import scala.language.implicitConversions

trait PlaySlick {
  def db(implicit app: Application) = Database.forDataSource(play.api.db.DB.getDataSource())
}

/**
 * 数据库连接对象
 */
object AppDB extends PlaySlick with scala.slick.driver.MySQLDriver {
}

object PlayCache {
  def remove(keys: String*)(implicit app: Application) {
    keys.foreach (key => play.api.cache.Cache.remove(key)(app))
  }
}

trait PlayCache {
  implicit def fromCache(cache: Cache.type) = PlayCache
}