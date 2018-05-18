package com.ppb.rabbitconsumer

import org.scalatest.{FlatSpec, Matchers}

class AmqpHeaderSpec extends FlatSpec with Matchers {

  behavior of "AmqpHeaderSpec"

  it should "toHeaders" in {
    AmqpHeader.toHeaders(List("a=1234")) should contain (("a", "1234"))
  }

}
