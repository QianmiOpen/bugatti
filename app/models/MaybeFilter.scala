package models

import scala.slick.lifted.CanBeQueryCondition

/**
 * optionally filter on a column with a supplied predicate
 */
case class MaybeFilter[X, Y](val query: scala.slick.lifted.Query[X, Y]) {
  def filter[T, R:CanBeQueryCondition](data: Option[T])(f: T => X => R) = {
    data.map(v => MaybeFilter(query.filter(f(v)))).getOrElse(this)
  }
}