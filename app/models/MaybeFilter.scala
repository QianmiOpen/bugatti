package models

import scala.slick.driver.H2Driver.simple._

/**
 * optionally filter on a column with a supplied predicate
 */
case class MaybeFilter[X, Y](val query: scala.slick.lifted.Query[X, Y, Seq]) {
  def filteredBy(op: Option[_])(f:(X) => Column[Option[Boolean]]) = {
    op map { o => MaybeFilter(query.filter(f)) } getOrElse { this }
  }
}