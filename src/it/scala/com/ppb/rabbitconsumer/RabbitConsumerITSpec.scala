package com.ppb.rabbitconsumer

import argonaut._
import Argonaut._
import org.scalatest.{FlatSpec, Matchers}
import org.slf4j.LoggerFactory
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._
import com.typesafe.config.{Config, ConfigFactory}

class RabbitConsumerITSpec extends FlatSpec with Matchers {

  private val logger = LoggerFactory.getLogger(RabbitConsumer.getClass)

  it should "read docker local configuration files and process it" in {
    val filename: String = (RabbitConsumer.getConfigs("local")).config(1).getString("fileName").replaceFirst("^~", System.getProperty("user.home"))
    RabbitConsumer.local
    for (line <- Source.fromFile(filename).getLines) {
          logger.info(line)
     }
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
