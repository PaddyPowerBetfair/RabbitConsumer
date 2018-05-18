package com.ppb.rabbitconsumer

import org.scalatest.{FlatSpec, Matchers}

class RabbitPublisherSpec extends FlatSpec with Matchers {

  behavior of "RabbitPublisherSpec"

  it should "toCsvRow" in {

    val x: Option[CsvRow] = CsvRow.toCsvRow("0,1,2,3,4,5")
    x should contain (CsvRow("0", "1", "2", "3", "4", "5"))
  }

}
