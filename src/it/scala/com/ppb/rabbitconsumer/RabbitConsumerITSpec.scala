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


  it should "read docker local configuration files and process it" in {
    RabbitConsumer.local
  }
}
