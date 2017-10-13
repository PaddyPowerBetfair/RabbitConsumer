package com.ppb.rabbitconsumer

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Failure

class RabbitConnectionSpec extends FlatSpec with Matchers {

  behavior of "RabbitConnection"

  it should "convert valid json into a json object" in {
    val input = """{"key":"value"}"""
    RabbitConnection.asJson(input.getBytes).get.toString() should be (input)
  }

  it should "convert invalid json into a json object" in {
    val input = "not valid json"
    RabbitConnection.asJson(input.getBytes) shouldBe a[Failure[_]]
  }

}
