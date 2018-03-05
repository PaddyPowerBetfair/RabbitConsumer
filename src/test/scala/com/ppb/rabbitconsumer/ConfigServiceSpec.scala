package com.ppb.rabbitconsumer

import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._


class ConfigServiceSpec extends FlatSpec with Matchers {

  behavior of "ConfigService"

  val config: Config = ConfigFactory.load("local").getConfigList("amqp.connections").asScala.toList.head

  it should "read the exchange from the Config" in {
    ConfigService.readExchange(config) should be("myExchange")
  }

  it should "read the queue from the Config" in {
    ConfigService.readQueue(config) should be("myQueue")
  }

  it should "read the routing key from the Config" in {
    ConfigService.readRoutingKey(config) should be("myRoutingKey")
  }
}
