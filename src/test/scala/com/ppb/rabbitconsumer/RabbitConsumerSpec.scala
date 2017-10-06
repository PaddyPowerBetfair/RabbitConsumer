package com.ppb.rabbitconsumer

import argonaut._
import Argonaut._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class RabbitConsumerSpec extends FlatSpec with Matchers {

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  behavior of "RabbitConsumer"

  it should "receive all messages" in new RabbitConsumerFixture {
    val message = RabbitConsumer.receiveAll(receiveOneMessage).toSource.runLog.run
    message should have size 1
  }

  it should "read configuration files" in {
    val config: Configurations = RabbitConsumer.getConfigs("local")
    config.name should be ("local")
    config.config should have size 2
  }
}

trait RabbitConsumerFixture {

  var times = 0

  val receiveOneMessage: () => Try[Json] = () =>
    if (times == 0) {
      times = 1
      Success("".asJson)
    } else {
      Failure(new Exception())
    }
}
