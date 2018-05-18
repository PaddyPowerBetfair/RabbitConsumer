package com.ppb.rabbitconsumer

import com.typesafe.config.Config

object ConfigService {
  def readExchange(config: Config): String = config.getString("exchangeName")
  def readQueue(config: Config): String = config.getString("queue")
  def readRoutingKey(config: Config): String = config.getString("routingKey")
  def getInputFilename(config: Config): String =
    config.getString("input").replaceFirst("^~", System.getProperty("user.home"))
  def getOutputFilename(config: Config): String =
    config.getString("output").replaceFirst("^~", System.getProperty("user.home"))
}
