package com.ppb.rabbitconsumer

import shapeless.{Generic, HList}
import shapeless.ops.traversable.FromTraversable

import scala.collection.GenTraversable

case class CsvRow(id: String, scoreHome: String, scoreAway: String, eventCode: String, currentPeriodTime: String, eventStateCode: String)

object CsvRow {
  class FromListToCaseClass[T] {
    def apply[R <: HList](l: GenTraversable[_])
                         (implicit gen: Generic.Aux[T, R], tl: FromTraversable[R]): Option[T] =
      tl(l).map(gen.from)
  }
  private def fromListToCaseClass[T] = new FromListToCaseClass[T]

  def toCsvRow(row: String): Option[CsvRow] = {
    fromListToCaseClass[CsvRow](row.split(",").toList)
  }

}
